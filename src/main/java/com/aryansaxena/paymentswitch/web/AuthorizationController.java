package com.aryansaxena.paymentswitch.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aryansaxena.paymentswitch.iso8583.Fields;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Codec;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Message;
import com.aryansaxena.paymentswitch.repository.TransactionRepository;
import com.aryansaxena.paymentswitch.service.AuthorizationService;
import com.aryansaxena.paymentswitch.service.PanMasking;
import com.aryansaxena.paymentswitch.service.ResponseCode;
import com.aryansaxena.paymentswitch.web.dto.AuthorizationResponse;
import com.aryansaxena.paymentswitch.web.dto.TransactionView;

@RestController
@RequestMapping("/api/v1")
public class AuthorizationController {

  private final Iso8583Codec codec;
  private final AuthorizationService authorizationService;
  private final TransactionRepository transactionRepository;

  public AuthorizationController(
      Iso8583Codec codec,
      AuthorizationService authorizationService,
      TransactionRepository transactionRepository) {
    this.codec = codec;
    this.authorizationService = authorizationService;
    this.transactionRepository = transactionRepository;
  }

  @PostMapping(
      value = "/authorizations",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public AuthorizationResponse authorize(@RequestBody String rawMessage) {
    Iso8583Message request = codec.decode(rawMessage.trim());
    Iso8583Message response = authorizationService.authorize(request);
    String responseCode = response.get(Fields.RESPONSE_CODE);
    return new AuthorizationResponse(
        codec.encode(response),
        response.getMti(),
        response.get(Fields.STAN),
        responseCode,
        ResponseCode.APPROVED.equals(responseCode) ? "APPROVED" : "DECLINED",
        response.find(Fields.AUTH_ID).orElse(null),
        response.find(Fields.PAN).map(PanMasking::mask).orElse(null),
        Long.parseLong(response.find(Fields.AMOUNT).orElse("0")),
        response.find(Fields.CURRENCY).orElse("000"));
  }

  @GetMapping("/transactions")
  public List<TransactionView> recentTransactions() {
    return transactionRepository.findTop50ByOrderByCreatedAtDesc().stream()
        .map(TransactionView::from)
        .toList();
  }

  @GetMapping("/transactions/{stan}")
  public TransactionView byStan(@PathVariable String stan) {
    return transactionRepository
        .findFirstByStanOrderByCreatedAtDesc(stan)
        .map(TransactionView::from)
        .orElseThrow(() -> new IllegalArgumentException("No transaction for STAN " + stan));
  }
}
