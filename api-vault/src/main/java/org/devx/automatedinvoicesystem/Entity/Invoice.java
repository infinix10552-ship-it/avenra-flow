package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id" ,nullable = false)
    private Organization organization;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "s3_file_url", nullable = false, length = 500)
    private String s3FileUrl;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "cgst")
    private  String cgst;

    @Column(name = "sgst")
    private String sgst;

    // FINANCIAL METADATA (Extracted by AI)

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "invoice_date")
    private java.time.LocalDate invoiceDate;

    @Column(name = "category")
    private String category;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus Status;


    public enum ProcessingStatus {

        PENDING,      // Uploaded, waiting for RabbitMQ to pick it up
        PROCESSING,   // Python script is currently processing the invoice
        COMPLETED,    // Processing finished successfully, data extracted and stored in DB
        FAILED        // Processing failed (e.g., due to file corruption, unsupported format, or extraction errors)

    }




}
