package com.revpay.repository;

import com.revpay.model.Card;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUser(User user);

    Optional<Card> findByUserAndIsDefaultTrue(User user);

    boolean existsByUserAndIsDefaultTrue(User user);

    @Modifying
    @Query(value = "UPDATE cards SET is_default = FALSE WHERE user_id = :userId AND is_default = TRUE", nativeQuery = true)
    int clearDefaultForUser(@Param("userId") Long userId);


}
