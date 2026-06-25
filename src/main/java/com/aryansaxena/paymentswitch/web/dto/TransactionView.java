package com.aryansaxena.paymentswitch.web.dto;

import java.time.Instant;

import com.aryansaxena.paymentswitch.domain.Transaction;

/** Read-only projection of a persisted {@link Transaction}. */
public record TransactionView(
    Long id,
    String mti,
    String stan,
    String maskedPan,
    long amountMinorUnits,
    String currency,
    String responseCode,
    String status,
    String approvalCode,
    Instant createdAt) {

  public static TransactionView from(Transaction tx) {
    return new TransactionView(
        tx.getId(),
        tx.getMti(),
        tx.getStan(),
        tx.getMaskedPan(),
        tx.getAmountMinorUnits(),
        tx.getCurrency(),
        tx.getResponseCode(),
        tx.getStatus().name(),
        tx.getApprovalCode(),
        tx.getCreatedAt());
  }
}
