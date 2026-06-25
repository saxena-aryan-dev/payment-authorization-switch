package com.aryansaxena.paymentswitch.iso8583;

/** Raised when a message cannot be parsed, validated or built per the ISO 8583 field rules. */
public class Iso8583Exception extends RuntimeException {

  public Iso8583Exception(String message) {
    super(message);
  }
}
