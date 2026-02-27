package com.revpay.repository;

import com.revpay.model.BusinessLoan;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessLoanRepository extends JpaRepository<BusinessLoan, Long> {
    List<BusinessLoan> findByBusinessUserOrderByAppliedAtDesc(User businessUser);
}
