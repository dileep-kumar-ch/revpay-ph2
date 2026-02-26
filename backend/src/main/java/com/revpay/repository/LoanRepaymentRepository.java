package com.revpay.repository;

import com.revpay.model.LoanRepayment;
import com.revpay.model.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    List<LoanRepayment> findByStatusAndDueDateLessThanEqual(RepaymentStatus status, LocalDate dueDate);
}
