package com.aryansaxena.paymentswitch.web.dto;

/** JSON view returned to the acquirer after an authorization request is switched. */
public record AuthorizationResponse(
    String responseMessage,
    String mti,
    String stan,
    String responseCode,
    String status,
    String approvalCode,
    String maskedPan,
    long amountMinorUnits,
    String currency) {}
