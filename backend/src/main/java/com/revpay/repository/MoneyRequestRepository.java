package com.revpay.repository;

import com.revpay.model.MoneyRequest;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {
    List<MoneyRequest> findByPayerOrRequester(User payer, User requester);

    List<MoneyRequest> findByPayer(User payer);

    List<MoneyRequest> findByRequester(User requester);

    @Query(value = """
            SELECT
                mr.id,
                rq.username AS requester_username,
                rq.full_name AS requester_full_name,
                rq.email AS requester_email,
                py.username AS payer_username,
                py.full_name AS payer_full_name,
                py.email AS payer_email,
                mr.amount,
                mr.note,
                mr.status,
                mr.created_at,
                CASE WHEN mr.payer_id = :userId THEN 'INCOMING' ELSE 'OUTGOING' END AS direction
            FROM money_requests mr
            JOIN users rq ON rq.id = mr.requester_id
            JOIN users py ON py.id = mr.payer_id
            WHERE mr.requester_id = :userId OR mr.payer_id = :userId
            ORDER BY mr.created_at DESC
            """, nativeQuery = true)
    List<Object[]> findRequestRowsByUserId(@Param("userId") Long userId);
}
