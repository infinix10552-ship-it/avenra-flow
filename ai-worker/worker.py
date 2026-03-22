import pika
import json
import os
import tempfile
import requests
from PIL import Image, ImageEnhance
import pdf2image
import pytesseract
from datetime import datetime
import threading
from flask import Flask

# 1. THE DUMMY WEB SERVER (RENDER HACK)

app = Flask(__name__)

@app.route('/')
def health_check():
    return "Avenra AI Worker is alive and listening to CloudAMQP!", 200

# 2. CLOUD CONFIGURATION & SECRETS
# Pass the full CloudAMQP URL here (amqps://user:pass@host/vhost)

AMQP_URL = os.environ.get('CLOUDAMQP_URL', 'amqp://guest:guest@localhost:5672/')
QUEUE_NAME = os.environ.get('QUEUE_NAME', 'invoice_queue')

# Default to Linux 'tesseract' command, override if on Windows locally
TESSERACT_CMD = os.environ.get('TESSERACT_CMD', 'tesseract')
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD

JAVA_WEBHOOK_URL = os.environ.get('JAVA_WEBHOOK_URL', "http://localhost:8081/api/v1/webhook/invoice-complete")
GROQ_API_KEY = os.environ.get('GROQ_API_KEY', 'YOUR_GROQ_KEY_HERE')
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

# 3. CORE PROCESSING FUNCTIONS

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
        for i, page in enumerate(pages):
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

def parse_invoice_data(raw_text):
    print("[*] Engaging Groq Cloud (llama-3.1-8b-instant) for Cognitive Extraction...")
    prompt = f"""
    You are an elite FinTech data extraction AI.
    Read the following OCR invoice text and extract the financial data.

    STRICT RULES:
    1. Output ONLY a valid JSON object. 
    2. The JSON must contain exactly these four keys: "vendorName", "totalAmount", "invoiceDate", "category".
    3. "totalAmount" MUST be a pure mathematical float (e.g., 1500.50). Remove commas and currency symbols.
    4. "invoiceDate" MUST be strictly in YYYY-MM-DD format. Default to "{datetime.today().strftime('%Y-%m-%d')}" if missing.
    5. "category" MUST be exactly one of: SOFTWARE, TRAVEL, HARDWARE, SERVICES, OFFICE_SUPPLIES, MISCELLANEOUS.

    RAW OCR TEXT:
    {raw_text}
    """
    try:
        headers = {
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": "llama-3.1-8b-instant", 
            "messages": [
                {"role": "system", "content": "You are a JSON-only data extraction engine. You only output valid JSON objects."},
                {"role": "user", "content": prompt}
            ],
            "response_format": {"type": "json_object"}, 
            "temperature": 0.0 
        }

        response = requests.post(GROQ_URL, headers=headers, json=payload)
        
        if response.status_code != 200:
            print(f"\n[❌] GROQ REJECTED THE REQUEST: {response.text}\n")
            
        response.raise_for_status()
        ai_output = response.json()['choices'][0]['message']['content']
        structured_data = json.loads(ai_output)

        return {
            "vendorName": str(structured_data.get("vendorName", "Unknown Vendor"))[:50],
            "totalAmount": float(structured_data.get("totalAmount", 0.0)),
            "invoiceDate": str(structured_data.get("invoiceDate", datetime.today().strftime('%Y-%m-%d'))),
            "category": str(structured_data.get("category", "MISCELLANEOUS"))
        }

    except Exception as e:
        print(f"[❌] AI Extraction Failed: {e}. Engaging Fallback Shield.")
        return {
            "vendorName": "AI Parsing Error",
            "totalAmount": 0.0,
            "invoiceDate": datetime.today().strftime('%Y-%m-%d'),
            "category": "MISCELLANEOUS"
        }

def send_to_java(payload):
    print(f"[*] Firing Webhook to Java: {JAVA_WEBHOOK_URL}")
    response = requests.post(JAVA_WEBHOOK_URL, json=payload)
    response.raise_for_status()
    print(f"[✅] Java Acknowledged: {response.text}")

# 4. RABBITMQ CONSUMER LOGIC

def process_invoice(ch, method, properties, body):
    try:
        message = json.loads(body)
        invoice_id = message.get('invoiceId')
        file_url = message.get('fileUrl')
        organization_id = message.get('organizationId') 
        
        print(f"\n[🚀] NEW JOB RECEIVED! Invoice ID: {invoice_id}")
        
        ext = os.path.splitext(file_url)[1] 
        fd, temp_path = tempfile.mkstemp(suffix=ext)
        os.close(fd) 
        
        try:
            download_file(file_url, temp_path)
            raw_text = extract_text_from_file(temp_path)
            structured_data = parse_invoice_data(raw_text)
            
            webhook_payload = {
                "invoiceId": invoice_id,
                "organizationId": organization_id,
                "vendorName": structured_data["vendorName"],
                "totalAmount": structured_data["totalAmount"],
                "invoiceDate": structured_data["invoiceDate"],
                "category": structured_data["category"]
            }
            
            print("\n================ FINAL WEBHOOK PAYLOAD ==============")
            print(json.dumps(webhook_payload, indent=4))
            print("====================================================\n")
            
            send_to_java(webhook_payload)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print("[*] Sent ACK to RabbitMQ. Ready for next job.")
            
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    except Exception as e:
        print(f"[❌] AI Processing crash: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def start_worker():
    print(f"[*] AI Worker Booting Up... Connecting to CloudAMQP")
    params = pika.URLParameters(AMQP_URL)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    channel.queue_declare(queue=QUEUE_NAME, durable=True)
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=process_invoice, auto_ack=False)
    
    try:
        channel.start_consuming()
    except Exception as e:
        print(f"\n[*] Worker interrupted: {e}")
        channel.stop_consuming()
    connection.close()

# 5. MULTI-THREADING IGNITION

if __name__ == '__main__':
    # Start the RabbitMQ consumer in a background thread
    worker_thread = threading.Thread(target=start_worker, daemon=True)
    worker_thread.start()
    
    # Start the Flask web server on the main thread
    port = int(os.environ.get('PORT', 10000))
    app.run(host='0.0.0.0', port=port)