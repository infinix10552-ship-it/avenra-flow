# RabbitMQ Message Flow Diagram

## Complete System Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         SPRING BOOT APPLICATION                            │
│                       (Automated Invoice System)                            │
│                                                                              │
│  ┌─────────────────┐         ┌──────────────────────┐                      │
│  │  InvoiceService │         │ InvoiceController    │                      │
│  │  (upload file)  │────────▶│  (POST /api/upload)  │                      │
│  └─────────────────┘         └──────────────────────┘                      │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │  STEP 1: Process Upload & Save to S3                               │  │
│  │  - Receive MultipartFile                                            │  │
│  │  - Save file to AWS S3                                              │  │
│  │  - Create Invoice entity in database                                │  │
│  └──────────────────────────┬──────────────────────────────────────────┘  │
│                             │                                               │
│                             ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │  STEP 2: Publish Message to RabbitMQ                                │ │
│  │                                                                       │ │
│  │  InvoiceMessagePublisher.sendInvoiceToQueue()                       │ │
│  │  │                                                                   │ │
│  │  ├─ Create message payload:                                         │ │
│  │  │  {                                                               │ │
│  │  │    "invoiceId": "uuid",                                          │ │
│  │  │    "organizationId": "uuid",                                     │ │
│  │  │    "fileUrl": "s3://bucket/key"                                  │ │
│  │  │  }                                                               │ │
│  │  │                                                                   │ │
│  │  ├─ Call: rabbitTemplate.convertAndSend(                            │ │
│  │  │         "invoice_exchange",                                      │ │
│  │  │         "invoice.process.routing.key",                           │ │
│  │  │         messagePayload                                           │ │
│  │  │       )                                                          │ │
│  │  │                                                                   │ │
│  │  └─ RabbitTemplate applies:                                         │ │
│  │     Jackson2JsonMessageConverter                                    │ │
│  │     └─ Map ──────▶ JSON String                                      │ │
│  │        {invoiceId: "...",     {                                     │ │
│  │         organizationId: "...",  "invoiceId": "...",                 │ │
│  │         fileUrl: "..."}       "organizationId": "...",              │ │
│  │                                 "fileUrl": "..."                    │ │
│  │                               }                                     │ │
│  └──────────────────────────┬───────────────────────────────────────────┘ │
└─────────────────────────────┼──────────────────────────────────────────────┘
                              │
                              │ (JSON Bytes)
                              │
                    ┌─────────▼─────────┐
                    │   RABBITMQ BROKER │
                    │                   │
                    │ ┌───────────────┐ │
                    │ │    Exchanges: │ │
                    │ │    DirectEx.  │ │
                    │ └───────┬───────┘ │
                    │         │         │
                    │    ┌────▼──────┐  │
                    │    │  Bindings │  │
                    │    │  (routing)│  │
                    │    └────┬──────┘  │
                    │         │         │
                    │    ┌────▼──────────────┐
                    │    │ Queue:            │
                    │    │ invoice_queue     │
                    │    │ [JSON message]    │
                    │    └────┬─────────────┘
                    │         │
                    └─────────┼──────────────┘
                              │
                              │ (JSON Bytes)
                              │
┌─────────────────────────────▼──────────────────────────────────────────────┐
│                         PYTHON WORKER PROCESS                              │
│                         (ai-worker/worker.py)                              │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │  STEP 3: Receive Message from RabbitMQ                            │   │
│  │                                                                    │   │
│  │  start_worker()                                                  │   │
│  │  │                                                               │   │
│  │  ├─ Connect to RabbitMQ (localhost:5672)                        │   │
│  │  │                                                               │   │
│  │  ├─ Declare queue: invoice_queue                               │   │
│  │  │                                                               │   │
│  │  ├─ Set QoS: prefetch_count=1                                  │   │
│  │  │                                                               │   │
│  │  └─ Start consuming with auto_ack=False                        │   │
│  │     (Manual acknowledgement mode)                               │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │  STEP 4: Process Invoice (Callback Function)                      │   │
│  │                                                                    │   │
│  │  process_invoice(ch, method, properties, body)                  │   │
│  │  │                                                               │   │
│  │  ├─ Receive raw body (bytes)                                    │   │
│  │  │  body = b'{"invoiceId":"...","organizationId":"...","fileUrl":"..."}'
│  │  │                                                               │   │
│  │  ├─ Decode bytes to UTF-8 string                               │   │
│  │  │  body = body.decode('utf-8')                                │   │
│  │  │  # Result: '{"invoiceId":"...","organizationId":"...","fileUrl":"..."}' 
│  │  │                                                               │   │
│  │  ├─ Parse JSON string to Python dict                           │   │
│  │  │  message = json.loads(body)                                │   │
│  │  │  # Result: {                                                │   │
│  │  │  #   'invoiceId': '...',                                    │   │
│  │  │  #   'organizationId': '...',                               │   │
│  │  │  #   'fileUrl': '...'                                       │   │
│  │  │  # }                                                        │   │
│  │  │                                                               │   │
│  │  ├─ Extract fields                                              │   │
│  │  │  invoice_id = message.get('invoiceId')                     │   │
│  │  │  org_id = message.get('organizationId')                    │   │
│  │  │  file_url = message.get('fileUrl')                         │   │
│  │  │                                                               │   │
│  │  ├─ Download file from S3                                       │   │
│  │  │  # (Implementation would add this)                           │   │
│  │  │                                                               │   │
│  │  ├─ Run AI/OCR/NLP models                                        │   │
│  │  │  • Extract text from PDF                                     │   │
│  │  │  • Parse invoice data                                        │   │
│  │  │  • Store results in database                                 │   │
│  │  │                                                               │   │
│  │  ├─ Send ACK (success acknowledgement)                           │   │
│  │  │  ch.basic_ack(delivery_tag=method.delivery_tag)            │   │
│  │  │  # Tells RabbitMQ: "I processed this message successfully"  │   │
│  │  │                                                               │   │
│  │  ├─ On error: Send NACK (negative acknowledgement)             │   │
│  │  │  ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
│  │  │  # Tells RabbitMQ: "Put this message back in the queue"    │   │
│  │  │                                                               │   │
│  │  └─ Print success message & wait for next job                   │   │
│  │     "[✅] AI Extraction Complete. Data parsed successfully."    │   │
│  │                                                                    │   │
│  └────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Message Transformation Timeline

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────────┐
│    JAVA      │      │   RABBITMQ       │      │     PYTHON       │
│              │      │                  │      │                  │
│ Map object:  │      │  JSON bytes:     │      │  Dict object:    │
│ {            │      │  b'{            │      │  {               │
│   invoiceId: │─────▶│    "invoiceId":  │─────▶│    invoiceId:    │
│   "abc123",  │      │    "abc123",     │      │    "abc123",     │
│   org Id:    │      │    "organizationId": 
│   "org456",  │      │    "org456",     │      │    organizationId│
│   fileUrl:   │      │    "fileUrl":    │      │    "org456",     │
│   "s3://..." │      │    "s3://..."    │      │    fileUrl:      │
│ }            │      │  }'              │      │    "s3://..."    │
│              │      │                  │      │  }               │
│ (Serialized) │      │ (Transmitted)    │      │ (Deserialized)   │
└──────────────┘      └──────────────────┘      └──────────────────┘
       │                       │                        │
       │ Jackson2Json          │ Network Transfer       │ json.loads()
       │ MessageConverter      │                        │
       │ .convertAndSend()     │                        │
       │                       │                        │
       └───────────────────────┴────────────────────────┘
                        ✅ JSON Message Format
```

## The Critical Fix Point

```
RabbitMQConfig.java (Lines 48-57):

    @Bean
    public RabbitTemplate rabbitTemplate(
        org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
        MessageConverter jackson2JsonMessageConverter  ← Injected converter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);  ← ✅ THE FIX
        //                           ↑
        //     Without this line, messages weren't converted to JSON!
        //     Now messages are automatically serialized to JSON format.
        template.setDefaultReceiveQueue(INVOICE_QUEUE);
        return template;
    }
```

## Message Lifecycle

```
1. CREATION
   └─ Map<String, Object> created in InvoiceMessagePublisher
      {invoiceId, organizationId, fileUrl}

2. SERIALIZATION (★ FIX HERE ★)
   └─ Jackson2JsonMessageConverter.toMessage()
      └─ Converts Map → JSON String → Message object

3. TRANSMISSION
   └─ RabbitTemplate sends to RabbitMQ

4. QUEUING
   └─ Message stored in invoice_queue (JSON format)

5. RECEIVING
   └─ Python worker.process_invoice() receives Message

6. DESERIALIZATION
   └─ json.loads(body.decode('utf-8'))
      └─ Converts JSON String → Python Dict

7. PROCESSING
   └─ Extract fields and process invoice

8. ACKNOWLEDGEMENT
   └─ Send ACK to RabbitMQ
      └─ Message removed from queue
```

## Error Flow (Before Fix)

```
Map created
    ↓
❌ No converter applied
    ↓
Message sent as raw serialized bytes (NOT JSON)
    ↓
RabbitMQ stores non-JSON data
    ↓
Python worker receives bytes
    ↓
json.loads() FAILS ❌
    ↓
json.JSONDecodeError exception
    ↓
System crash 💥
```

## Success Flow (After Fix)

```
Map created
    ↓
✅ Jackson2JsonMessageConverter applied
    ↓
Message sent as JSON string
    ↓
RabbitMQ stores JSON data
    ↓
Python worker receives JSON bytes
    ↓
json.loads() SUCCEEDS ✅
    ↓
Extract invoiceId, organizationId, fileUrl
    ↓
Process invoice with AI models
    ↓
Send ACK to RabbitMQ
    ↓
System works perfectly ✅
```
