package com.revpay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceStatusSchemaRepair {

    private static final String EXPECTED_ENUM = "enum('SENT','PAID','OVERDUE')";
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void repairInvoiceStatusColumn() {
        try {
            String columnType = jdbcTemplate.queryForObject("""
                    SELECT COLUMN_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'invoices'
                      AND COLUMN_NAME = 'status'
                    """, String.class);

            if (columnType == null) {
                return;
            }

            if (!EXPECTED_ENUM.equalsIgnoreCase(columnType.trim())) {
                log.warn("Repairing invoices.status column definition from [{}] to [{}]", columnType, EXPECTED_ENUM);
                jdbcTemplate.execute(
                        "ALTER TABLE invoices MODIFY COLUMN status ENUM('SENT','PAID','OVERDUE') NOT NULL");
            }

            jdbcTemplate.update("""
                    UPDATE invoices
                    SET status = 'SENT'
                    WHERE status IS NULL OR status = '' OR status NOT IN ('SENT', 'PAID', 'OVERDUE')
                    """);
        } catch (Exception ex) {
            log.warn("Invoice status schema repair skipped: {}", ex.getMessage());
        }
    }
}
