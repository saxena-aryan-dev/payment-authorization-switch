package com.aryansaxena.paymentswitch.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LuhnValidatorTest {

  private final LuhnValidator validator = new LuhnValidator();

  @Test
  void acceptsValidPan() {
    assertThat(validator.isValid("4111111111111111")).isTrue();
  }

  @Test
  void rejectsPanThatFailsChecksum() {
    assertThat(validator.isValid("4111111111111112")).isFalse();
  }

  @Test
  void rejectsNonNumericOrNull() {
    assertThat(validator.isValid("4111-1111-1111-1111")).isFalse();
    assertThat(validator.isValid(null)).isFalse();
    assertThat(validator.isValid("123")).isFalse();
  }
}
