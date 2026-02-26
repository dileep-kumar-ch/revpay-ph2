package com.revpay.repository;

import com.revpay.model.BusinessBankAccount;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessBankAccountRepository extends JpaRepository<BusinessBankAccount, Long> {
    List<BusinessBankAccount> findByBusinessUser(User businessUser);

    boolean existsByBusinessUserAndIsDefaultTrue(User businessUser);

    @Modifying
    @Query("UPDATE BusinessBankAccount b SET b.isDefault = false WHERE b.businessUser.id = :userId")
    void clearDefaultForUser(Long userId);
}
