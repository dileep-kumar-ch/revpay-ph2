package com.revpay.repository;

import com.revpay.model.Invoice;
import com.revpay.model.InvoiceStatus;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByBusinessUserOrderByCreatedAtDesc(User businessUser);

    List<Invoice> findByBusinessUserAndStatusOrderByCreatedAtDesc(User businessUser, InvoiceStatus status);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    List<Invoice> findByCustomerPhoneOrCustomerEmailOrCustomerExternalIdOrderByCreatedAtDesc(
            String customerPhone, String customerEmail, String customerExternalId);
}
