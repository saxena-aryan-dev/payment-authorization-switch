package com.aryansaxena.paymentswitch.service;

/** ISO 8583 field 39 response codes used by this switch. */
public final class ResponseCode {

  public static final String APPROVED = "00";
  public static final String DO_NOT_HONOR = "05";
  public static final String INVALID_AMOUNT = "13";
  public static final String INVALID_CARD = "14";
  public static final String INSUFFICIENT_FUNDS = "51";
  public static final String EXPIRED_CARD = "54";
  public static final String EXCEEDS_LIMIT = "61";

  private ResponseCode() {}
}
