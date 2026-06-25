package com.aryansaxena.paymentswitch.iso8583;

import java.util.Map;

/** Registry of the ISO 8583 data elements supported by this switch. */
public final class Fields {

  public static final int PAN = 2;
  public static final int PROCESSING_CODE = 3;
  public static final int AMOUNT = 4;
  public static final int TRANSMISSION_DATETIME = 7;
  public static final int STAN = 11;
  public static final int LOCAL_TIME = 12;
  public static final int LOCAL_DATE = 13;
  public static final int EXPIRY_DATE = 14;
  public static final int AUTH_ID = 38;
  public static final int RESPONSE_CODE = 39;
  public static final int TERMINAL_ID = 41;
  public static final int MERCHANT_ID = 42;
  public static final int CURRENCY = 49;

  private static final Map<Integer, FieldSpec> SPECS =
      Map.ofEntries(
          Map.entry(PAN, new FieldSpec(PAN, FieldType.LLVAR, 19, "Primary Account Number")),
          Map.entry(
              PROCESSING_CODE, new FieldSpec(PROCESSING_CODE, FieldType.FIXED, 6, "Processing Code")),
          Map.entry(AMOUNT, new FieldSpec(AMOUNT, FieldType.FIXED, 12, "Amount, Transaction")),
          Map.entry(
              TRANSMISSION_DATETIME,
              new FieldSpec(TRANSMISSION_DATETIME, FieldType.FIXED, 10, "Transmission Date and Time")),
          Map.entry(STAN, new FieldSpec(STAN, FieldType.FIXED, 6, "System Trace Audit Number")),
          Map.entry(LOCAL_TIME, new FieldSpec(LOCAL_TIME, FieldType.FIXED, 6, "Local Transaction Time")),
          Map.entry(LOCAL_DATE, new FieldSpec(LOCAL_DATE, FieldType.FIXED, 4, "Local Transaction Date")),
          Map.entry(EXPIRY_DATE, new FieldSpec(EXPIRY_DATE, FieldType.FIXED, 4, "Expiration Date")),
          Map.entry(AUTH_ID, new FieldSpec(AUTH_ID, FieldType.FIXED, 6, "Authorization ID Response")),
          Map.entry(RESPONSE_CODE, new FieldSpec(RESPONSE_CODE, FieldType.FIXED, 2, "Response Code")),
          Map.entry(
              TERMINAL_ID, new FieldSpec(TERMINAL_ID, FieldType.FIXED, 8, "Card Acceptor Terminal ID")),
          Map.entry(
              MERCHANT_ID, new FieldSpec(MERCHANT_ID, FieldType.FIXED, 15, "Card Acceptor ID Code")),
          Map.entry(CURRENCY, new FieldSpec(CURRENCY, FieldType.FIXED, 3, "Currency Code, Transaction")));

  private Fields() {}

  public static FieldSpec spec(int field) {
    FieldSpec spec = SPECS.get(field);
    if (spec == null) {
      throw new Iso8583Exception("Unsupported data element: " + field);
    }
    return spec;
  }

  public static boolean isSupported(int field) {
    return SPECS.containsKey(field);
  }
}
