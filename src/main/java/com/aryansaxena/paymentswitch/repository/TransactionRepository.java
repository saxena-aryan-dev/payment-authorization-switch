package com.aryansaxena.paymentswitch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aryansaxena.paymentswitch.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  Optional<Transaction> findFirstByStanOrderByCreatedAtDesc(String stan);

  List<Transaction> findTop50ByOrderByCreatedAtDesc();
}
