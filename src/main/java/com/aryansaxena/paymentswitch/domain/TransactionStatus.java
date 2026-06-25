package com.aryansaxena.paymentswitch.domain;

/** Outcome of an authorization request after the decisioning rules have been applied. */
public enum TransactionStatus {
  APPROVED,
  DECLINED
}
