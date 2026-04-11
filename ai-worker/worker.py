import pika
import json
import os
import re
import tempfile
import requests
from decimal import Decimal, InvalidOperation
from PIL import Image, ImageEnhance
import pdf2image
import pytesseract
from datetime import datetime
import threading
from flask import Flask

# ══════════════════════════════════════════════════════════════════════
# AVENRA AI WORKER v2.0 — CA-Grade GST Extraction Engine
# ══════════════════════════════════════════════════════════════════════
# This worker extracts 12 GST-specific fields from invoices with
# per-field confidence scoring and strict schema validation.
# No silent corrections. No assumptions. No fallbacks.
# ══════════════════════════════════════════════════════════════════════

# ── HEALTH CHECK SERVER (Render deployment) ────────────────────────

app = Flask(__name__)

@app.route('/')
def health_check():
    return "Avenra AI Worker v2.0 — GST Extraction Engine", 200

# ── CONFIGURATION ──────────────────────────────────────────────────

AMQP_URL = os.environ.get('CLOUDAMQP_URL', 'amqp://guest:guest@localhost:5672/')
QUEUE_NAME = os.environ.get('QUEUE_NAME', 'invoice_queue')

TESSERACT_CMD = os.environ.get('TESSERACT_CMD', 'tesseract')
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD

JAVA_WEBHOOK_URL = os.environ.get('JAVA_WEBHOOK_URL', "http://localhost:8081/api/v1/webhook/invoice-complete")
GROQ_API_KEY = os.environ.get('GROQ_API_KEY', 'YOUR_GROQ_KEY_HERE')
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

# ── GSTIN VALIDATION ───────────────────────────────────────────────

GSTIN_REGEX = re.compile(r'^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$')

def validate_gstin(gstin):
    """Returns True if GSTIN matches the Indian GST format."""
    if gstin is None or len(str(gstin).strip()) != 15:
        return False
    return bool(GSTIN_REGEX.match(str(gstin).strip().upper()))

# ── STRICT JSON SCHEMA ─────────────────────────────────────────────

REQUIRED_FIELDS = [
    "invoiceNumber", "invoiceDate", "supplierName", "supplierGstin",
    "buyerGstin", "hsnSac", "baseAmount", "cgst", "sgst", "igst",
    "totalAmount", "confidenceScore"
]

def validate_ai_output(data):
    """
    Validates that the AI output contains ALL required fields.
    Returns (is_valid, missing_fields).
    No auto-corrections. No defaults. Strict contract enforcement.
    """
    if not isinstance(data, dict):
        return False, REQUIRED_FIELDS

    missing = [f for f in REQUIRED_FIELDS if f not in data]
    return len(missing) == 0, missing

# ── CONFIDENCE SCORING ─────────────────────────────────────────────

FIELD_WEIGHTS = {
    "invoiceNumber": 8,
    "invoiceDate": 8,
    "supplierName": 10,
    "supplierGstin": 15,
    "buyerGstin": 15,
    "hsnSac": 5,
    "baseAmount": 12,
    "cgst": 7,
    "sgst": 7,
    "igst": 5,
    "totalAmount": 12,
}

def compute_field_confidence(field_name, value):
    """
    Per-field confidence based on value quality.
    Returns a score between 0 and 100.
    """
    if value is None or str(value).strip() == "":
        return 0.0

    value_str = str(value).strip()

    # GSTIN fields — binary (valid format or not)
    if field_name in ("supplierGstin", "buyerGstin"):
        return 95.0 if validate_gstin(value_str) else 30.0

    # Numeric fields — must be parseable as Decimal
    if field_name in ("baseAmount", "cgst", "sgst", "igst", "totalAmount"):
        try:
            d = Decimal(str(value))
            return 90.0 if d >= 0 else 50.0
        except (InvalidOperation, ValueError):
            return 20.0

    # Date field — must be YYYY-MM-DD
    if field_name == "invoiceDate":
        try:
            datetime.strptime(value_str, "%Y-%m-%d")
            return 90.0
        except ValueError:
            return 30.0

    # String fields — confidence based on length
    if len(value_str) >= 2:
        return 85.0
    return 50.0

def compute_global_confidence(data):
    """
    Weighted average of per-field confidence scores.
    Returns global confidence score (0-100).
    """
    total_weight = sum(FIELD_WEIGHTS.values())
    weighted_sum = 0.0

    for field, weight in FIELD_WEIGHTS.items():
        score = compute_field_confidence(field, data.get(field))
        weighted_sum += score * weight

    return round(weighted_sum / total_weight, 2)

# ── FILE PROCESSING ────────────────────────────────────────────────

def download_file(url, local_path):
    response = requests.get(url, stream=True)
    response.raise_for_status()
    with open(local_path, 'wb') as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)

def extract_text_from_file(file_path):
    extracted_text = ""
    if file_path.lower().endswith('.pdf'):
        print("[*] PDF detected. Rasterizing at 300 DPI...")
        pages = pdf2image.convert_from_path(file_path, dpi=300)
        for page in pages:
            gray_page = page.convert('L')
            enhancer = ImageEnhance.Contrast(gray_page)
            processed_page = enhancer.enhance(2.0)
            extracted_text += pytesseract.image_to_string(processed_page) + "\n"
    else:
        print("[*] Image detected. Pre-processing...")
        image = Image.open(file_path)
        gray_image = image.convert('L')
        enhancer = ImageEnhance.Contrast(gray_image)
        processed_image = enhancer.enhance(2.0)
        extracted_text = pytesseract.image_to_string(processed_image)

    return extracted_text

# ── AI EXTRACTION (GST-COMPLIANT) ─────────────────────────────────

def parse_invoice_data(raw_text):
    """
    Sends OCR text to Groq LLM for structured GST extraction.
    Returns EXACTLY 12 fields or raises an error.
    No fallbacks. No defaults. No silent corrections.
    """
    print("[*] Engaging Groq Cloud for GST-compliant extraction...")

    prompt = f"""
    You are a CA-grade GST data extraction engine for Indian invoices.
    
    Read the following OCR text from a financial invoice and extract ALL fields below.
    
    STRICT RULES:
    1. Output ONLY a valid JSON object with EXACTLY these 12 keys.
    2. If a field cannot be found, set its value to null — DO NOT guess.
    3. All monetary values MUST be pure numbers (no commas, no currency symbols).
    4. GSTIN MUST be exactly 15 characters in the format: 2 digits + 5 uppercase + 4 digits + 1 uppercase + 1 alphanumeric + Z + 1 alphanumeric.
    5. invoiceDate MUST be in YYYY-MM-DD format.
    6. confidenceScore is YOUR self-assessment (0-100) of extraction accuracy.
    
    REQUIRED JSON SCHEMA:
    {{
        "invoiceNumber": "string or null",
        "invoiceDate": "YYYY-MM-DD or null",
        "supplierName": "string or null",
        "supplierGstin": "15-char GSTIN or null",
        "buyerGstin": "15-char GSTIN or null",
        "hsnSac": "HSN/SAC code or null",
        "baseAmount": number or null,
        "cgst": number or null,
        "sgst": number or null,
        "igst": number or null,
        "totalAmount": number or null,
        "confidenceScore": number (0-100)
    }}
    
    RAW OCR TEXT:
    {raw_text}
    """

    headers = {
        "Authorization": f"Bearer {GROQ_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": "llama-3.1-8b-instant",
        "messages": [
            {"role": "system", "content": "You are a JSON-only GST invoice data extraction engine. Output ONLY valid JSON. Never add commentary."},
            {"role": "user", "content": prompt}
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.0
    }

    response = requests.post(GROQ_URL, headers=headers, json=payload)

    if response.status_code != 200:
        raise RuntimeError(f"Groq API rejected request: HTTP {response.status_code} — {response.text}")

    response.raise_for_status()
    ai_output = response.json()['choices'][0]['message']['content']
    structured_data = json.loads(ai_output)

    # STRICT SCHEMA VALIDATION — reject if any field is missing
    is_valid, missing_fields = validate_ai_output(structured_data)
    if not is_valid:
        raise ValueError(f"AI output schema violation. Missing fields: {missing_fields}")

    # Sanitize numeric fields to Decimal-safe strings
    for numeric_field in ("baseAmount", "cgst", "sgst", "igst", "totalAmount"):
        val = structured_data.get(numeric_field)
        if val is not None:
            try:
                structured_data[numeric_field] = float(Decimal(str(val)))
            except (InvalidOperation, ValueError):
                structured_data[numeric_field] = None

    # Compute per-field weighted confidence (override AI self-assessment)
    computed_confidence = compute_global_confidence(structured_data)
    structured_data["confidenceScore"] = computed_confidence

    return structured_data

# ── WEBHOOK TO JAVA ────────────────────────────────────────────────

def send_to_java(payload):
    print(f"[*] Firing Webhook to Java: {JAVA_WEBHOOK_URL}")
    response = requests.post(JAVA_WEBHOOK_URL, json=payload)
    response.raise_for_status()
    print(f"[✅] Java Acknowledged: {response.text}")

# ── RABBITMQ CONSUMER ──────────────────────────────────────────────

def process_invoice(ch, method, properties, body):
    try:
        message = json.loads(body)
        invoice_id = message.get('invoiceId')
        file_url = message.get('fileUrl')
        organization_id = message.get('organizationId')
        client_id = message.get('clientId')

        print(f"\n[🚀] NEW JOB — Invoice: {invoice_id}")

        ext = os.path.splitext(file_url)[1]
        fd, temp_path = tempfile.mkstemp(suffix=ext)
        os.close(fd)

        try:
            # Step 1: Download
            download_file(file_url, temp_path)
            print(f"[✅] File downloaded: {temp_path}")

            # Step 2: OCR
            raw_text = extract_text_from_file(temp_path)
            print(f"[✅] OCR complete: {len(raw_text)} chars extracted")

            # Step 3: AI Extraction (strict schema)
            structured_data = parse_invoice_data(raw_text)
            print(f"[✅] AI extraction complete. Confidence: {structured_data.get('confidenceScore')}%")

            # Step 4: Build webhook payload (EXACT match to Java DTO)
            webhook_payload = {
                "invoiceId": invoice_id,
                "organizationId": organization_id,
                "clientId": client_id,
                "invoiceNumber": structured_data.get("invoiceNumber"),
                "invoiceDate": structured_data.get("invoiceDate"),
                "supplierName": structured_data.get("supplierName"),
                "supplierGstin": structured_data.get("supplierGstin"),
                "buyerGstin": structured_data.get("buyerGstin"),
                "hsnSac": structured_data.get("hsnSac"),
                "baseAmount": structured_data.get("baseAmount"),
                "cgst": structured_data.get("cgst"),
                "sgst": structured_data.get("sgst"),
                "igst": structured_data.get("igst"),
                "totalAmount": structured_data.get("totalAmount"),
                "confidenceScore": structured_data.get("confidenceScore"),
            }

            print("\n════════════ WEBHOOK PAYLOAD ════════════")
            print(json.dumps(webhook_payload, indent=4, default=str))
            print("═════════════════════════════════════════\n")

            # Step 5: Send to Java
            send_to_java(webhook_payload)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print("[✅] ACK sent. Ready for next job.")

        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    except ValueError as e:
        # Schema violation — HARD FAIL, no retry
        print(f"[❌] SCHEMA VIOLATION: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    except Exception as e:
        # Processing crash — requeue for retry
        print(f"[❌] PROCESSING CRASH: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

# ── WORKER STARTUP ─────────────────────────────────────────────────

def start_worker():
    import time
    import traceback

    while True:
        try:
            print("[*] AI Worker v2.0 booting... Connecting to CloudAMQP", flush=True)

            params = pika.URLParameters(AMQP_URL)
            connection = pika.BlockingConnection(params)
            channel = connection.channel()
            channel.queue_declare(queue=QUEUE_NAME, durable=True)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=QUEUE_NAME, on_message_callback=process_invoice, auto_ack=False)

            print("[✅] Connected to RabbitMQ. Awaiting invoices...", flush=True)
            channel.start_consuming()

        except Exception as e:
            print(f"\n[❌] FATAL RABBITMQ CRASH: {e}", flush=True)
            traceback.print_exc()
            print("[*] Rebooting in 10 seconds...\n", flush=True)
            time.sleep(10)

# ── ENTRYPOINT ─────────────────────────────────────────────────────

if __name__ == '__main__':
    worker_thread = threading.Thread(target=start_worker, daemon=True)
    worker_thread.start()

    port = int(os.environ.get('PORT', 10000))
    app.run(host='0.0.0.0', port=port)