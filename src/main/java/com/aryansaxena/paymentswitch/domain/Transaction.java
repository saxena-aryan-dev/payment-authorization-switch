package com.aryansaxena.paymentswitch.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** Persisted record of a single authorization request and its decision. */
@Entity
@Table(
    name = "transactions",
    indexes = {@Index(name = "idx_tx_stan", columnList = "stan")})
@Getter
@Setter
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 4)
  private String mti;

  @Column(name = "stan", nullable = false, length = 6)
  private String stan;

  @Column(name = "masked_pan", nullable = false, length = 19)
  private String maskedPan;

  @Column(name = "amount_minor_units", nullable = false)
  private long amountMinorUnits;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "response_code", nullable = false, length = 2)
  private String responseCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TransactionStatus status;

  @Column(name = "approval_code", length = 6)
  private String approvalCode;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
