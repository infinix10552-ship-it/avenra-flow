package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Entity.Invoice.ProcessingStatus;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.ProcessingLog;
import org.devx.automatedinvoicesystem.Repository.ClientRepo;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.ProcessingLogRepo;
import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.devx.automatedinvoicesystem.Service.WebSocketNotificationService;
import org.devx.automatedinvoicesystem.Specification.InvoiceSpecification;
import org.devx.automatedinvoicesystem.Validation.GstinValidator;
import org.devx.automatedinvoicesystem.Validation.TaxCalculationValidator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final FileStorageService fileStorageService;
    private final InvoiceRepo invoiceRepository;
    private final OrganizationRepo organizationRepository;
    private final ClientRepo clientRepository;
    private final ProcessingLogRepo processingLogRepo;
    private final InvoiceMessagePublisher invoiceMessagePublisher;
    private final WebSocketNotificationService notificationService;

    public InvoiceServiceImpl(FileStorageService fileStorageService,
                              InvoiceRepo invoiceRepository,
                              OrganizationRepo organizationRepository,
                              ClientRepo clientRepository,
                              ProcessingLogRepo processingLogRepo,
                              InvoiceMessagePublisher invoiceMessagePublisher,
                              WebSocketNotificationService notificationService) {
        this.fileStorageService = fileStorageService;
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
        this.clientRepository = clientRepository;
        this.processingLogRepo = processingLogRepo;
        this.invoiceMessagePublisher = invoiceMessagePublisher;
        this.notificationService = notificationService;
    }

    // ── SINGLE UPLOAD (Legacy — no client scope) ──────────────────────

    @Override
    @Transactional
    public Invoice processInvoiceUpload(MultipartFile file, UUID organizationId) {
        return processInvoiceUpload(file, organizationId, null);
    }

    // ── SINGLE UPLOAD (v2.0 — client scoped) ──────────────────────────

    @Override
    @Transactional
    public Invoice processInvoiceUpload(MultipartFile file, UUID organizationId, UUID clientId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // Cryptographic deduplication
        String fileFingerprint = generateFileHash(file);
        if (invoiceRepository.existsByFileHashAndOrganizationId(fileFingerprint, organizationId)) {
            throw new IllegalArgumentException("Duplicate detected: This exact document has already been uploaded.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + organizationId));

        Client client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + clientId));
        }

        String s3Url = fileStorageService.uploadFile(file);

        Invoice newInvoice = new Invoice();
        newInvoice.setOrganization(organization);
        newInvoice.setClient(client);
        newInvoice.setOriginalFileName(file.getOriginalFilename());
        newInvoice.setS3FileUrl(s3Url);
        newInvoice.setStatus(ProcessingStatus.PENDING);
        newInvoice.setFileHash(fileFingerprint);
        newInvoice.setRetryCount(0);

        Invoice savedInvoice = invoiceRepository.save(newInvoice);

        // Audit log
        processingLogRepo.save(ProcessingLog.create(
                savedInvoice, ProcessingLog.LogLevel.INFO, "UPLOAD",
                "Invoice uploaded: " + file.getOriginalFilename()
        ));

        invoiceMessagePublisher.sendInvoiceToQueue(savedInvoice);

        return savedInvoice;
    }

    // ── BULK UPLOAD ───────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId) {
        return processBulkUpload(zipFile, organizationId, null);
    }

    @Override
    @Transactional
    public Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId, UUID clientId) {
        if (zipFile.isEmpty() || !zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Please upload a valid .zip file.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Client client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + clientId));
        }

        int successCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".pdf")) {
                    zis.closeEntry();
                    continue;
                }

                try {
                    byte[] fileBytes = zis.readAllBytes();
                    String fileFingerprint = generateHashFromBytes(fileBytes);

                    if (invoiceRepository.existsByFileHashAndOrganizationId(fileFingerprint, organizationId)) {
                        duplicateCount++;
                        zis.closeEntry();
                        continue;
                    }

                    String s3Url = fileStorageService.uploadFileBytes(fileBytes, entry.getName());

                    Invoice newInvoice = new Invoice();
                    newInvoice.setOrganization(organization);
                    newInvoice.setClient(client);
                    newInvoice.setOriginalFileName(entry.getName());
                    newInvoice.setS3FileUrl(s3Url);
                    newInvoice.setStatus(ProcessingStatus.PENDING);
                    newInvoice.setFileHash(fileFingerprint);
                    newInvoice.setRetryCount(0);

                    Invoice savedInvoice = invoiceRepository.save(newInvoice);

                    processingLogRepo.save(ProcessingLog.create(
                            savedInvoice, ProcessingLog.LogLevel.INFO, "BULK_UPLOAD",
                            "Uploaded from ZIP: " + entry.getName()
                    ));

                    invoiceMessagePublisher.sendInvoiceToQueue(savedInvoice);
                    successCount++;

                } catch (Exception e) {
                    System.err.println("Failed to process file in ZIP: " + entry.getName() + " - " + e.getMessage());
                    failedCount++;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Catastrophic failure reading ZIP stream: " + e.getMessage());
        }

        Map<String, Integer> report = new HashMap<>();
        report.put("successful", successCount);
        report.put("duplicates_skipped", duplicateCount);
        report.put("failed", failedCount);
        return report;
    }

    // ── WEBHOOK COMPLETION (v2.0 — Full Validation) ───────────────────

    @Override
    @Transactional
    public void completeInvoiceProcessing(WebhookPayload payload) {
        Invoice invoice = invoiceRepository.findById(payload.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + payload.getInvoiceId()));

        // 1. Map all AI-extracted fields to entity
        invoice.setInvoiceNumber(payload.getInvoiceNumber());
        invoice.setInvoiceDate(payload.getInvoiceDate());
        invoice.setSupplierName(payload.getSupplierName());
        invoice.setSupplierGstin(payload.getSupplierGstin());
        invoice.setBuyerGstin(payload.getBuyerGstin());
        invoice.setHsnSacCode(payload.getHsnSac());
        invoice.setBaseTaxableAmount(payload.getBaseAmount());
        invoice.setCgst(payload.getCgst());
        invoice.setSgst(payload.getSgst());
        invoice.setIgst(payload.getIgst());
        invoice.setTotalAmount(payload.getTotalAmount());
        invoice.setAiConfidenceScore(payload.getConfidenceScore());

        processingLogRepo.save(ProcessingLog.create(
                invoice, ProcessingLog.LogLevel.INFO, "AI_EXTRACTION",
                "AI data received. Confidence: " + payload.getConfidenceScore()
        ));

        // 2. Run validation pipeline
        List<String> validationFailures = new ArrayList<>();

        // 2a. GSTIN validation
        if (payload.getSupplierGstin() != null && !payload.getSupplierGstin().isBlank()) {
            if (!GstinValidator.isValid(payload.getSupplierGstin())) {
                validationFailures.add("Invalid supplier GSTIN format: " + payload.getSupplierGstin());
            }
        }
        if (payload.getBuyerGstin() != null && !payload.getBuyerGstin().isBlank()) {
            if (!GstinValidator.isValid(payload.getBuyerGstin())) {
                validationFailures.add("Invalid buyer GSTIN format: " + payload.getBuyerGstin());
            }
        }

        // 2b. Tax math validation (STRICT — no rounding, no corrections)
        boolean taxMathValid = TaxCalculationValidator.isValid(
                payload.getBaseAmount(),
                payload.getCgst(),
                payload.getSgst(),
                payload.getIgst(),
                payload.getTotalAmount()
        );

        if (!taxMathValid) {
            BigDecimal mismatch = TaxCalculationValidator.getMismatchAmount(
                    payload.getBaseAmount(), payload.getCgst(),
                    payload.getSgst(), payload.getIgst(), payload.getTotalAmount()
            );
            validationFailures.add("Tax math mismatch: base + cgst + sgst + igst != total. Diff: " + mismatch);
        }

        // 2c. Confidence threshold check
        if (payload.getConfidenceScore() != null && payload.getConfidenceScore() < 85.0) {
            validationFailures.add("AI confidence below threshold: " + payload.getConfidenceScore() + "% (min: 85%)");
        }

        // 3. Determine final status
        if (!validationFailures.isEmpty()) {
            invoice.setStatus(ProcessingStatus.REQUIRES_MANUAL_REVIEW);

            for (String failure : validationFailures) {
                processingLogRepo.save(ProcessingLog.create(
                        invoice, ProcessingLog.LogLevel.WARN, "VALIDATION_FAILED", failure
                ));
            }

            System.out.println("⚠️ [VALIDATION] Invoice " + invoice.getId()
                    + " → REQUIRES_MANUAL_REVIEW (" + validationFailures.size() + " issues)");
        } else {
            invoice.setStatus(ProcessingStatus.COMPLETED);

            processingLogRepo.save(ProcessingLog.create(
                    invoice, ProcessingLog.LogLevel.INFO, "VALIDATION_PASSED",
                    "All validations passed. Tax math correct. Confidence: " + payload.getConfidenceScore() + "%"
            ));

            System.out.println("✅ [VALIDATION] Invoice " + invoice.getId() + " → COMPLETED");
        }

        // 4. Persist
        invoiceRepository.save(invoice);

        // 5. Fire WebSocket notification
        notificationService.sendInvoiceStatusUpdate(
                payload.getOrganizationId(),
                invoice.getId(),
                invoice.getStatus().name()
        );
    }

    // ── REVIEW QUEUE METHODS ──────────────────────────────────────────

    @Override
    @Transactional
    public Invoice approveInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != ProcessingStatus.REQUIRES_MANUAL_REVIEW) {
            throw new IllegalStateException("Invoice is not in review status. Current: " + invoice.getStatus());
        }

        invoice.setStatus(ProcessingStatus.COMPLETED);
        Invoice saved = invoiceRepository.save(invoice);

        processingLogRepo.save(ProcessingLog.create(
                saved, ProcessingLog.LogLevel.INFO, "MANUAL_APPROVED",
                "Invoice manually approved by user"
        ));

        return saved;
    }

    @Override
    @Transactional
    public Invoice updateAndApproveInvoice(UUID invoiceId, WebhookPayload correctedData) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != ProcessingStatus.REQUIRES_MANUAL_REVIEW) {
            throw new IllegalStateException("Invoice is not in review status. Current: " + invoice.getStatus());
        }

        // Apply corrections
        if (correctedData.getInvoiceNumber() != null) invoice.setInvoiceNumber(correctedData.getInvoiceNumber());
        if (correctedData.getInvoiceDate() != null) invoice.setInvoiceDate(correctedData.getInvoiceDate());
        if (correctedData.getSupplierName() != null) invoice.setSupplierName(correctedData.getSupplierName());
        if (correctedData.getSupplierGstin() != null) invoice.setSupplierGstin(correctedData.getSupplierGstin());
        if (correctedData.getBuyerGstin() != null) invoice.setBuyerGstin(correctedData.getBuyerGstin());
        if (correctedData.getHsnSac() != null) invoice.setHsnSacCode(correctedData.getHsnSac());
        if (correctedData.getBaseAmount() != null) invoice.setBaseTaxableAmount(correctedData.getBaseAmount());
        if (correctedData.getCgst() != null) invoice.setCgst(correctedData.getCgst());
        if (correctedData.getSgst() != null) invoice.setSgst(correctedData.getSgst());
        if (correctedData.getIgst() != null) invoice.setIgst(correctedData.getIgst());
        if (correctedData.getTotalAmount() != null) invoice.setTotalAmount(correctedData.getTotalAmount());

        invoice.setStatus(ProcessingStatus.COMPLETED);
        Invoice saved = invoiceRepository.save(invoice);

        processingLogRepo.save(ProcessingLog.create(
                saved, ProcessingLog.LogLevel.INFO, "MANUAL_CORRECTED",
                "Invoice manually corrected and approved by user"
        ));

        return saved;
    }

    // ── QUERY METHODS ─────────────────────────────────────────────────

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> getAllInvoices(UUID organizationId) {
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Override
    public List<Invoice> getInvoicesByClient(UUID clientId) {
        return invoiceRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    public List<Invoice> searchInvoices(InvoiceSearchFilter filter) {
        return invoiceRepository.findAll(
                InvoiceSpecification.getSearchSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────

    private String generateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Fatal Error: SHA-256 algorithm missing from JVM", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file for security hashing", e);
        }
    }

    private String generateHashFromBytes(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 missing", e);
        }
    }
}