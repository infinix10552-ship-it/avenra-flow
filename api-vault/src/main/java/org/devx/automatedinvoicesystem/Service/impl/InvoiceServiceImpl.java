package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.DTO.InvoiceSearchFilter;
import org.devx.automatedinvoicesystem.DTO.WebhookPayload;
import org.devx.automatedinvoicesystem.Entity.Invoice;
import org.devx.automatedinvoicesystem.Entity.Organization;
import org.devx.automatedinvoicesystem.Repository.InvoiceRepo;
import org.devx.automatedinvoicesystem.Repository.OrganizationRepo;
import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.devx.automatedinvoicesystem.Service.InvoiceService;
import org.devx.automatedinvoicesystem.Service.WebSocketNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.devx.automatedinvoicesystem.Entity.Invoice.ProcessingStatus;
import org.devx.automatedinvoicesystem.Specification.InvoiceSpecification;
import org.springframework.data.domain.Sort;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final FileStorageService fileStorageService;
    private final InvoiceRepo invoiceRepository;
    private final OrganizationRepo organizationRepository;
    private final InvoiceMessagePublisher invoiceMessagePublisher;
    private final WebSocketNotificationService notificationService;

    public InvoiceServiceImpl(FileStorageService fileStorageService,
                              InvoiceRepo invoiceRepository,
                              OrganizationRepo organizationRepository,
                              InvoiceMessagePublisher invoiceMessagePublisher, WebSocketNotificationService notificationService) {
        this.fileStorageService = fileStorageService;
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
        this.invoiceMessagePublisher = invoiceMessagePublisher;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Invoice processInvoiceUpload(MultipartFile file, UUID organizationId) {
        // 1. Validation
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // 2. CRYPTOGRAPHIC DEDUPLICATION (The Fortress Guard)
        // We hash the file BEFORE we make expensive network calls to AWS S3 or PostgreSQL
        String fileFingerprint = generateFileHash(file);

        if (invoiceRepository.existsByFileHashAndOrganizationId(fileFingerprint, organizationId)) {
            // Block the upload immediately to save compute resources
            throw new IllegalArgumentException("Duplicate detected: This exact document has already been uploaded to your organization.");
        }

        // 3. Fetch Organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + organizationId));

        // 4. Upload to Cloud (S3)
        String s3Url = fileStorageService.uploadFile(file);

        // 5. Build and Save Entity
        Invoice newInvoice = new Invoice();
        newInvoice.setOrganization(organization);
        newInvoice.setOriginalFileName(file.getOriginalFilename());
        newInvoice.setS3FileUrl(s3Url);
        newInvoice.setStatus(Invoice.ProcessingStatus.PENDING);

        // Save the fingerprint so future duplicates are blocked
        newInvoice.setFileHash(fileFingerprint);

        Invoice savedInvoice = invoiceRepository.save(newInvoice);

        // 6. Publish the job to RabbitMQ!
        invoiceMessagePublisher.sendInvoiceToQueue(savedInvoice);

        return savedInvoice;
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }


    public List<Invoice> getAllInvoices(UUID organizationId) {
        // THE FIX: Isolated and sorted query
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    // 2. Implement the Webhook Method


    @Override
    @Transactional
    public void completeInvoiceProcessing(WebhookPayload payload) {

        // 1. Find the pending invoice in the vault
        Invoice invoice = invoiceRepository.findById(payload.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + payload.getInvoiceId()));

        // 2. Unbox the DTO and map the AI's financial extractions to the database entity
        invoice.setStatus(Invoice.ProcessingStatus.COMPLETED);
        invoice.setVendorName(payload.getVendorName());
        invoice.setTotalAmount(payload.getTotalAmount());
        invoice.setInvoiceDate(payload.getInvoiceDate());
        invoice.setCategory(payload.getCategory());

        // 3. Save the rich financial data to PostgreSQL
        invoiceRepository.save(invoice);

        // 4. Fire the real-time UI update so the React dashboard renders the new numbers
        notificationService.sendInvoiceStatusUpdate(
                payload.getOrganizationId(),
                invoice.getId(),
                "COMPLETED"
        );

        System.out.println("✅ [WEBHOOK] Successfully saved financial data for Invoice: " + invoice.getId());
    }


    // PRIVATE HELPER: The SHA-256 Engine
    private String generateFileHash(MultipartFile file) {
        try {
            // Initialize the SHA-256 cryptography engine
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Read the raw physical bytes of the PDF and hash them
            byte[] hashBytes = digest.digest(file.getBytes());

            // Convert the raw bytes into a readable 64-character Hexadecimal string
            return String.format("%064x", new BigInteger(1, hashBytes));

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Fatal Error: SHA-256 algorithm missing from JVM", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file for security hashing", e);
        }
    }

    @Override
    @Transactional
    public Map<String, Integer> processBulkUpload(MultipartFile zipFile, UUID organizationId) {

        // 1. Validate it's actually a ZIP file
        if (zipFile.isEmpty() || !zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Please upload a valid .zip file.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // 2. Initialize our tracking counters for the React Report Card
        int successCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        // 3. THE CONVEYOR BELT (ZipInputStream)
        // We wrap this in a try-with-resources block so Java automatically closes the stream to prevent memory leaks.
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {

            ZipEntry entry;

            // Pull the next file off the belt until the belt is empty
            while ((entry = zis.getNextEntry()) != null) {

                // Ignore folders and non-PDF files
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".pdf")) {
                    zis.closeEntry();
                    continue;
                }

                try {
                    // Extract the bytes of this single PDF
                    byte[] fileBytes = zis.readAllBytes();

                    // THE FORTRESS GUARD (Silently skips duplicates)
                    String fileFingerprint = generateHashFromBytes(fileBytes);
                    if (invoiceRepository.existsByFileHashAndOrganizationId(fileFingerprint, organizationId)) {
                        duplicateCount++;
                        zis.closeEntry();
                        continue; // Skip this file and move to the next one
                    }

                    //             THE UPLOAD CLOUD HANDOFF
                    // Note: Your FileStorageService currently expects a MultipartFile.
                    // To keep things simple without refactoring your S3 service, we can wrap the raw bytes
                    // in a Custom MultipartFile adapter, OR if your S3 service accepts bytes, use that.
                    // Assuming your FileStorageService can be overloaded to take bytes and a filename:
                    String s3Url = fileStorageService.uploadFileBytes(fileBytes, entry.getName());

                    // SAVE TO DATABASE
                    Invoice newInvoice = new Invoice();
                    newInvoice.setOrganization(organization);
                    newInvoice.setOriginalFileName(entry.getName());
                    newInvoice.setS3FileUrl(s3Url);
                    newInvoice.setStatus(ProcessingStatus.PENDING);
                    newInvoice.setFileHash(fileFingerprint);

                    Invoice savedInvoice = invoiceRepository.save(newInvoice);

                    // FIRE TO RABBITMQ
                    invoiceMessagePublisher.sendInvoiceToQueue(savedInvoice);

                    successCount++;

                } catch (Exception e) {
                    System.err.println("Failed to process file in ZIP: " + entry.getName() + " - " + e.getMessage());
                    failedCount++;
                }

                // Explicitly tell Java to discard this file from RAM before grabbing the next one
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Catastrophic failure reading ZIP stream: " + e.getMessage());
        }

        // 4. Return the Report Card
        Map<String, Integer> report = new HashMap<>();
        report.put("successful", successCount);
        report.put("duplicates_skipped", duplicateCount);
        report.put("failed", failedCount);

        return report;
    }

    //OVERLOADED CRYPTOGRAPHY HELPER
    // We need a version of your hashing function that accepts raw byte arrays instead of MultipartFiles
    private String generateHashFromBytes(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 missing", e);
        }
    }

    @Override
    public List<Invoice> searchInvoices(InvoiceSearchFilter filter) {
        // We pass the dynamic specification to the repository, and tell it to sort the result by creation date in descending order so the newest invoices appear first in the React dashboard.
        return invoiceRepository.findAll(
                InvoiceSpecification.getSearchSpecification(filter),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}