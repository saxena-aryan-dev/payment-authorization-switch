package com.aryansaxena.paymentswitch.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aryansaxena.paymentswitch.domain.Transaction;
import com.aryansaxena.paymentswitch.domain.TransactionStatus;
import com.aryansaxena.paymentswitch.iso8583.Fields;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Exception;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Message;
import com.aryansaxena.paymentswitch.repository.TransactionRepository;

/**
 * Core switching logic: validates an authorization request (MTI 0100), applies a deterministic set
 * of decisioning rules, persists the transaction and builds the authorization response (MTI 0110).
 */
@Service
public class AuthorizationService {

  private static final String AUTH_REQUEST_MTI = "0100";
  private static final String AUTH_RESPONSE_MTI = "0110";
  private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  private final LuhnValidator luhnValidator;
  private final AccountService accountService;
  private final TransactionRepository transactionRepository;
  private final SecureRandom random = new SecureRandom();
  private final long perTransactionLimitMinorUnits;

  public AuthorizationService(
      LuhnValidator luhnValidator,
      AccountService accountService,
      TransactionRepository transactionRepository,
      @Value("${switch.per-transaction-limit-minor-units:500000}")
          long perTransactionLimitMinorUnits) {
    this.luhnValidator = luhnValidator;
    this.accountService = accountService;
    this.transactionRepository = transactionRepository;
    this.perTransactionLimitMinorUnits = perTransactionLimitMinorUnits;
  }

  @Transactional
  public Iso8583Message authorize(Iso8583Message request) {
    if (!AUTH_REQUEST_MTI.equals(request.getMti())) {
      throw new Iso8583Exception("Only authorization requests (MTI 0100) are supported");
    }
    String pan = required(request, Fields.PAN);
    String amountRaw = required(request, Fields.AMOUNT);
    required(request, Fields.STAN);

    long amount = parseAmount(amountRaw);
    String responseCode = decide(pan, amount, request.find(Fields.EXPIRY_DATE).orElse(null));

    Iso8583Message response = buildResponse(request, responseCode);
    persist(request, response, pan, amount, responseCode);
    return response;
  }

  private String decide(String pan, long amount, String expiry) {
    if (!luhnValidator.isValid(pan)) {
      return ResponseCode.INVALID_CARD;
    }
    if (isExpired(expiry)) {
      return ResponseCode.EXPIRED_CARD;
    }
    if (amount <= 0) {
      return ResponseCode.INVALID_AMOUNT;
    }
    if (amount > perTransactionLimitMinorUnits) {
      return ResponseCode.EXCEEDS_LIMIT;
    }
    if (accountService.isBlocked(pan)) {
      return ResponseCode.DO_NOT_HONOR;
    }
    if (accountService.balanceOf(pan) < amount) {
      return ResponseCode.INSUFFICIENT_FUNDS;
    }
    accountService.debit(pan, amount);
    return ResponseCode.APPROVED;
  }

  private boolean isExpired(String expiry) {
    if (expiry == null || expiry.length() != 4) {
      return false;
    }
    try {
      int year = 2000 + Integer.parseInt(expiry.substring(0, 2));
      int month = Integer.parseInt(expiry.substring(2, 4));
      YearMonth cardExpiry = YearMonth.of(year, month);
      return cardExpiry.isBefore(YearMonth.now(ZoneOffset.UTC));
    } catch (RuntimeException e) {
      return true;
    }
  }

  private Iso8583Message buildResponse(Iso8583Message request, String responseCode) {
    Iso8583Message response = new Iso8583Message(AUTH_RESPONSE_MTI);
    copyIfPresent(request, response, Fields.PAN);
    copyIfPresent(request, response, Fields.PROCESSING_CODE);
    copyIfPresent(request, response, Fields.AMOUNT);
    copyIfPresent(request, response, Fields.TRANSMISSION_DATETIME);
    copyIfPresent(request, response, Fields.STAN);
    copyIfPresent(request, response, Fields.CURRENCY);
    if (ResponseCode.APPROVED.equals(responseCode)) {
      response.set(Fields.AUTH_ID, approvalCode());
    }
    response.set(Fields.RESPONSE_CODE, responseCode);
    return response;
  }

  private void persist(
      Iso8583Message request,
      Iso8583Message response,
      String pan,
      long amount,
      String responseCode) {
    Transaction tx = new Transaction();
    tx.setMti(request.getMti());
    tx.setStan(request.get(Fields.STAN));
    tx.setMaskedPan(PanMasking.mask(pan));
    tx.setAmountMinorUnits(amount);
    tx.setCurrency(request.find(Fields.CURRENCY).orElse("000"));
    tx.setResponseCode(responseCode);
    tx.setStatus(
        ResponseCode.APPROVED.equals(responseCode)
            ? TransactionStatus.APPROVED
            : TransactionStatus.DECLINED);
    tx.setApprovalCode(response.find(Fields.AUTH_ID).orElse(null));
    tx.setCreatedAt(Instant.now());
    transactionRepository.save(tx);
  }

  private long parseAmount(String amountRaw) {
    try {
      return Long.parseLong(amountRaw);
    } catch (NumberFormatException e) {
      throw new Iso8583Exception("Field 4 (amount) must be numeric");
    }
  }

  private String required(Iso8583Message message, int field) {
    return message
        .find(field)
        .orElseThrow(() -> new Iso8583Exception("Missing mandatory field " + field));
  }

  private void copyIfPresent(Iso8583Message from, Iso8583Message to, int field) {
    from.find(field).ifPresent(value -> to.set(field, value));
  }

  private String approvalCode() {
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      sb.append(ALPHANUMERIC[random.nextInt(ALPHANUMERIC.length)]);
    }
    return sb.toString();
  }
}
