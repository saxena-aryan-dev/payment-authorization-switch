package com.aryansaxena.paymentswitch.service;

import org.springframework.stereotype.Component;

/** Validates a Primary Account Number against the Luhn (mod-10) checksum. */
@Component
public class LuhnValidator {

  public boolean isValid(String pan) {
    if (pan == null || !pan.matches("\\d{12,19}")) {
      return false;
    }
    int sum = 0;
    boolean alternate = false;
    for (int i = pan.length() - 1; i >= 0; i--) {
      int digit = pan.charAt(i) - '0';
      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }
      sum += digit;
      alternate = !alternate;
    }
    return sum % 10 == 0;
  }
}
