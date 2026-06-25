package com.aryansaxena.paymentswitch.service;

/** Utility for masking a PAN so only the last four digits are retained for storage and display. */
public final class PanMasking {

  private static final int VISIBLE_DIGITS = 4;

  private PanMasking() {}

  public static String mask(String pan) {
    if (pan == null || pan.length() <= VISIBLE_DIGITS) {
      return pan;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pan.length() - VISIBLE_DIGITS; i++) {
      sb.append('*');
    }
    sb.append(pan.substring(pan.length() - VISIBLE_DIGITS));
    return sb.toString();
  }
}
