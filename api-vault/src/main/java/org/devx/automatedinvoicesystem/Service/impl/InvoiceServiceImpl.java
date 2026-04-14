package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Client;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Entity.Invoice.ProcessingStatus;
import org.devx.automatedinvoicesystem.Entity.InvoiceAuditLog;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Entity.ProcessingLog;
import org.devx.automatedinvoicesystem.Repository.ClientRepo;
import org.devx.automatedinvoicesystem.Repository.InvoiceAuditLogRepo;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Repository.ProcessingLogRepo;
import org.devx.automatedinvoicesystem.Service.CurrencyConversionService;
import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.devx.automatedinvoicesystem.Service.WebSocketNotificationService;
import org.devx.automatedinvoicesystem.Specification.InvoiceSpecification;
import org.devx.automatedinvoicesystem.Validation.InvoiceValidationService;
import org.devx.automatedinvoicesystem.Validation.InvoiceValidationService.ValidationResult;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
    private final InvoiceAuditLogRepo auditLogRepo;
    private final InvoiceMessagePublisher invoiceMessagePublisher;
    private final WebSocketNotificationService notificationService;
    private final InvoiceValidationService validationService;
    private final CurrencyConversionService currencyConversionService;

    public InvoiceServiceImpl(FileStorageService fileStorageService,
                              InvoiceRepo invoiceRepository,
                              OrganizationRepo organizationRepository,
                              ClientRepo clientRepository,
                              ProcessingLogRepo processingLogRepo,
                              InvoiceAuditLogRepo auditLogRepo,
                              InvoiceMessagePublisher invoiceMessagePublisher,
                              WebSocketNotificationService notificationService,
                              InvoiceValidationService validationService,
                              CurrencyConversionService currencyConversionService) {
        this.fileStorageService = fileStorageService;
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
        this.clientRepository = clientRepository;
        this.processingLogRepo = processingLogRepo;
        this.auditLogRepo = auditLogRepo;
        this.invoiceMessagePublisher = invoiceMessagePublisher;
        this.notificationService = notificationService;
        this.validationService = validationService;
        this.currencyConversionService = currencyConversionService;
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

        // Cryptographic deduplication (file-level)
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

    // ── WEBHOOK COMPLETION (v2.1 — Centralized Validation Pipeline) ──

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
        invoice.setLedgerAccountName(payload.getLedgerAccountName());
        
        // Map multi-currency fields
        String detectedCurrency = payload.getCurrency() != null ? payload.getCurrency() : "INR";
        invoice.setOriginalCurrency(detectedCurrency);

        // LIVE FOREX ENGINE: Fetch real-time exchange rate for non-INR invoices
        if (!"INR".equalsIgnoreCase(detectedCurrency)) {
            BigDecimal liveRate = currencyConversionService.getExchangeRateToINR(detectedCurrency);
            invoice.setExchangeRate(liveRate);

            // Convert total amount to INR using the live rate
            BigDecimal totalInr = currencyConversionService.convertToINR(invoice.getTotalAmount(), detectedCurrency);
            invoice.setConvertedAmountInr(totalInr);

            processingLogRepo.save(ProcessingLog.create(
                    invoice, ProcessingLog.LogLevel.INFO, "FOREX_CONVERSION",
                    "Live conversion: " + detectedCurrency + " → INR at rate " + liveRate
                            + ". Total INR: " + totalInr
            ));
        } else {
            invoice.setExchangeRate(BigDecimal.ONE);
            invoice.setConvertedAmountInr(invoice.getTotalAmount());
        }

        processingLogRepo.save(ProcessingLog.create(
                invoice, ProcessingLog.LogLevel.INFO, "AI_EXTRACTION",
                "AI data received. Confidence: " + payload.getConfidenceScore()
                        + ". Ledger: " + payload.getLedgerAccountName()
        ));

        // 2. Run centralized validation pipeline (PRD §3)
        UUID clientId = invoice.getClient() != null ? invoice.getClient().getId() : null;
        ValidationResult result = validationService.validate(payload, clientId);

        // 3. Assign status based on validation result
        switch (result.status()) {
            case COMPLETED -> {
                invoice.setStatus(ProcessingStatus.COMPLETED);
                processingLogRepo.save(ProcessingLog.create(
                        invoice, ProcessingLog.LogLevel.INFO, "VALIDATION_PASSED",
                        "All validations passed. Tax math correct. Confidence: "
                                + payload.getConfidenceScore() + "%"
                ));
                System.out.println("✅ [VALIDATION] Invoice " + invoice.getId() + " → COMPLETED");
            }

            case REQUIRES_MANUAL_REVIEW -> {
                invoice.setStatus(ProcessingStatus.REQUIRES_MANUAL_REVIEW);
                for (String failure : result.failures()) {
                    processingLogRepo.save(ProcessingLog.create(
                            invoice, ProcessingLog.LogLevel.WARN, "VALIDATION_FAILED", failure
                    ));
                }
                System.out.println("⚠️ [VALIDATION] Invoice " + invoice.getId()
                        + " → REQUIRES_MANUAL_REVIEW (" + result.failures().size() + " issues)");
            }

            case FAILED -> {
                invoice.setStatus(ProcessingStatus.FAILED);
                invoice.setFailureReason(result.failureReason());
                for (String failure : result.failures()) {
                    processingLogRepo.save(ProcessingLog.create(
                            invoice, ProcessingLog.LogLevel.ERROR, "VALIDATION_FAILED", failure
                    ));
                }
                System.out.println("❌ [VALIDATION] Invoice " + invoice.getId()
                        + " → FAILED. Reason: " + result.failureReason());
            }
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

    // ── REVIEW QUEUE METHODS (PRD §5 — Audit Enforced) ───────────────

    @Override
    @Transactional
    public Invoice approveInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != ProcessingStatus.REQUIRES_MANUAL_REVIEW) {
            throw new IllegalStateException("Invoice is not in review status. Current: " + invoice.getStatus());
        }

        invoice.setModifiedAt(LocalDateTime.now());
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

        // PRD §5.1 & §5.3: Track every field change with audit log
        UUID modifiedBy = invoice.getModifiedBy();
        List<InvoiceAuditLog> auditEntries = new ArrayList<>();

        auditEntries.addAll(trackFieldChange(invoice, "invoiceNumber",
                invoice.getInvoiceNumber(), correctedData.getInvoiceNumber(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "invoiceDate",
                String.valueOf(invoice.getInvoiceDate()), String.valueOf(correctedData.getInvoiceDate()), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "supplierName",
                invoice.getSupplierName(), correctedData.getSupplierName(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "supplierGstin",
                invoice.getSupplierGstin(), correctedData.getSupplierGstin(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "buyerGstin",
                invoice.getBuyerGstin(), correctedData.getBuyerGstin(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "hsnSacCode",
                invoice.getHsnSacCode(), correctedData.getHsnSac(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "ledgerAccountName",
                invoice.getLedgerAccountName(), correctedData.getLedgerAccountName(), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "baseTaxableAmount",
                amountStr(invoice.getBaseTaxableAmount()), amountStr(correctedData.getBaseAmount()), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "cgst",
                amountStr(invoice.getCgst()), amountStr(correctedData.getCgst()), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "sgst",
                amountStr(invoice.getSgst()), amountStr(correctedData.getSgst()), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "igst",
                amountStr(invoice.getIgst()), amountStr(correctedData.getIgst()), modifiedBy));
        auditEntries.addAll(trackFieldChange(invoice, "totalAmount",
                amountStr(invoice.getTotalAmount()), amountStr(correctedData.getTotalAmount()), modifiedBy));

        // Apply corrections
        if (correctedData.getInvoiceNumber() != null) invoice.setInvoiceNumber(correctedData.getInvoiceNumber());
        if (correctedData.getInvoiceDate() != null) invoice.setInvoiceDate(correctedData.getInvoiceDate());
        if (correctedData.getSupplierName() != null) invoice.setSupplierName(correctedData.getSupplierName());
        if (correctedData.getSupplierGstin() != null) invoice.setSupplierGstin(correctedData.getSupplierGstin());
        if (correctedData.getBuyerGstin() != null) invoice.setBuyerGstin(correctedData.getBuyerGstin());
        if (correctedData.getHsnSac() != null) invoice.setHsnSacCode(correctedData.getHsnSac());
        if (correctedData.getLedgerAccountName() != null) invoice.setLedgerAccountName(correctedData.getLedgerAccountName());
        if (correctedData.getBaseAmount() != null) invoice.setBaseTaxableAmount(correctedData.getBaseAmount());
        if (correctedData.getCgst() != null) invoice.setCgst(correctedData.getCgst());
        if (correctedData.getSgst() != null) invoice.setSgst(correctedData.getSgst());
        if (correctedData.getIgst() != null) invoice.setIgst(correctedData.getIgst());
        if (correctedData.getTotalAmount() != null) invoice.setTotalAmount(correctedData.getTotalAmount());

        invoice.setModifiedAt(LocalDateTime.now());
        invoice.setStatus(ProcessingStatus.COMPLETED);

        Invoice saved = invoiceRepository.save(invoice);

        if (!auditEntries.isEmpty()) {
            auditLogRepo.saveAll(auditEntries);
        }

        processingLogRepo.save(ProcessingLog.create(
                saved, ProcessingLog.LogLevel.INFO, "MANUAL_CORRECTED",
                "Invoice manually corrected and approved. " + auditEntries.size() + " field(s) changed."
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
    public Invoice getInvoiceById(UUID invoiceId, UUID organizationId) {
        return invoiceRepository.findById(invoiceId)
                .filter(i -> i.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found or access denied"));
    }

    @Override
    @Transactional
    public void deleteInvoiceById(UUID invoiceId, UUID organizationId) {
        deleteInvoiceById(invoiceId, organizationId, null);
    }

    @Override
    @Transactional
    public void deleteInvoiceById(UUID invoiceId, UUID organizationId, String deletedByEmail) {
        try {
            Invoice invoice = getInvoiceById(invoiceId, organizationId);

            // SOFT DELETE: Mark as DELETED instead of removing from DB
            invoice.setStatus(ProcessingStatus.DELETED);
            invoice.setDeletedBy(deletedByEmail != null ? deletedByEmail : "SYSTEM");
            invoice.setDeletedAt(LocalDateTime.now());
            
            invoiceRepository.saveAndFlush(invoice);

            // Audit Log: Record deletion event
            try {
                processingLogRepo.save(ProcessingLog.create(
                        invoice, ProcessingLog.LogLevel.WARN, "SOFT_DELETED",
                        "Invoice soft-deleted by: " + (deletedByEmail != null ? deletedByEmail : "SYSTEM")
                ));
            } catch (Exception logError) {
                System.err.println("⚠️ [DELETE] Failed to write processing log, but invoice was deleted: " + logError.getMessage());
            }

            System.out.println("🗑️ [DELETE] Invoice " + invoiceId + " successfully soft-deleted by " + deletedByEmail);
        } catch (IllegalArgumentException e) {
            throw e; // Rethrow for controller to handle as 404/403
        } catch (Exception e) {
            System.err.println("❌ [FATAL DELETE ERROR] Details: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database integrity error during deletion: " + e.getMessage());
        }
    }

    @Override
    public List<Invoice> getDeletedInvoices(UUID organizationId) {
        return invoiceRepository.findByOrganizationIdAndStatus(organizationId, ProcessingStatus.DELETED);
    }

    @Override
    public List<Invoice> searchInvoices(InvoiceSearchFilter filter) {
        return invoiceRepository.findAll(
                InvoiceSpecification.getSearchSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    @Override
    public Map<String, Object> getDashboardAnalytics(UUID organizationId) {
        long totalInvoices = invoiceRepository.countByOrganizationId(organizationId);
        long completed = invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.COMPLETED);
        long pending = invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.PENDING) +
                       invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.PROCESSING);
        long needsReview = invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.REQUIRES_MANUAL_REVIEW);
        long failed = invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.FAILED);

        BigDecimal totalBilling = invoiceRepository.sumTotalAmountByOrganizationId(organizationId, Invoice.ProcessingStatus.COMPLETED);
        BigDecimal totalTax = invoiceRepository.sumTotalTaxByOrganizationId(organizationId, Invoice.ProcessingStatus.COMPLETED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInvoices", totalInvoices);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("needsReview", needsReview);
        stats.put("failed", failed);
        stats.put("totalBilling", totalBilling != null ? totalBilling : BigDecimal.ZERO);
        stats.put("totalTax", totalTax != null ? totalTax : BigDecimal.ZERO);

        return stats;
    }

    @Override
    public byte[] generateCsvExport(UUID organizationId) {
        List<Invoice> completedInvoices = invoiceRepository.findByOrganizationIdAndStatus(organizationId, Invoice.ProcessingStatus.COMPLETED);
        
        StringBuilder csv = new StringBuilder();
        // GSTR-2A / Tally Compliant Header
        csv.append("Date,Invoice No,Supplier,Supplier GSTIN,Buyer GSTIN,HSN/SAC,")
           .append("Base (Org),CGST (Org),SGST (Org),IGST (Org),Total (Org),")
           .append("Currency,Ex Rate,")
           .append("Base (INR),CGST (INR),SGST (INR),IGST (INR),Total (INR),")
           .append("Ledger\n");
        
        for (Invoice inv : completedInvoices) {
            BigDecimal rate = inv.getExchangeRate() != null ? inv.getExchangeRate() : BigDecimal.ONE;
            
            // Map original values
            BigDecimal bOrg = inv.getBaseTaxableAmount() != null ? inv.getBaseTaxableAmount() : BigDecimal.ZERO;
            BigDecimal cOrg = inv.getCgst() != null ? inv.getCgst() : BigDecimal.ZERO;
            BigDecimal sOrg = inv.getSgst() != null ? inv.getSgst() : BigDecimal.ZERO;
            BigDecimal iOrg = inv.getIgst() != null ? inv.getIgst() : BigDecimal.ZERO;
            BigDecimal tOrg = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;

            // Compute INR values
            BigDecimal bInr = bOrg.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal cInr = cOrg.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal sInr = sOrg.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal iInr = iOrg.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal tInr = inv.getConvertedAmountInr() != null ? inv.getConvertedAmountInr() : tOrg.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                inv.getInvoiceDate(),
                escapeCsv(inv.getInvoiceNumber()),
                escapeCsv(inv.getSupplierName()),
                inv.getSupplierGstin(),
                inv.getBuyerGstin(),
                escapeCsv(inv.getHsnSacCode()),
                bOrg, cOrg, sOrg, iOrg, tOrg,
                inv.getOriginalCurrency(),
                rate,
                bInr, cInr, sInr, iInr, tInr,
                escapeCsv(inv.getLedgerAccountName())
            ));
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────

    private List<InvoiceAuditLog> trackFieldChange(Invoice invoice, String fieldName,
                                                     String oldValue, String newValue,
                                                     UUID modifiedBy) {
        if (newValue == null || newValue.equals(oldValue) || "null".equals(newValue)) {
            return List.of();
        }
        return List.of(InvoiceAuditLog.create(invoice, fieldName, oldValue, newValue,
                modifiedBy != null ? modifiedBy : UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private String amountStr(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "null";
    }

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