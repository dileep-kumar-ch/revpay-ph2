package com.revpay.repository;

import com.revpay.model.Transaction;
import com.revpay.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet = :wallet OR t.receiverWallet = :wallet ORDER BY t.timestamp DESC")
    List<Transaction> findAllByWallet(Wallet wallet);
}
