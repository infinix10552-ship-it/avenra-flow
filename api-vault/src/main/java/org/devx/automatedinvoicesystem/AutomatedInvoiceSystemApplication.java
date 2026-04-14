package org.devx.automatedinvoicesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AutomatedInvoiceSystemApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void autoHealDatabaseConstraints() {
        try {
            // PostgreSQL hardcodes Enums into a CHECK constraint during initial table creation.
            // Spring Boot's 'update' DDL does not drop these checks when enums expand.
            // We forcefully drop the old constraint so the new 'DELETED' status can be written.
            jdbcTemplate.execute("ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check");
            System.out.println("✅ [AUTO-HEAL] Database constraints sanitized. 'DELETED' state is fully unblocked.");
        } catch (Exception e) {
            System.err.println("⚠️ [AUTO-HEAL] Constraint adjustment skipped: " + e.getMessage());
        }
    }

	public static void main(String[] args) {
		SpringApplication.run(AutomatedInvoiceSystemApplication.class, args);
		System.out.println("Automated Invoice System is running...");
	}

}
