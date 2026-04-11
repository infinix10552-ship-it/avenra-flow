package org.devx.automatedinvoicesystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processing_logs", indexes = {
        @Index(name = "idx_processing_log_invoice", columnList = "invoice_id"),
        @Index(name = "idx_processing_log_timestamp", columnList = "logged_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingLog extends Base {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }

    /**
     * Factory method for concise log creation.
     */
    public static ProcessingLog create(Invoice invoice, LogLevel level, String step, String message) {
        ProcessingLog log = new ProcessingLog();
        log.setInvoice(invoice);
        log.setLogLevel(level);
        log.setStepName(step);
        log.setMessage(message);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }
}
