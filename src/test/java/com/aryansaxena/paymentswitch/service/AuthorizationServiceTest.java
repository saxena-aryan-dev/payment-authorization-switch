package com.aryansaxena.paymentswitch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.aryansaxena.paymentswitch.domain.Transaction;
import com.aryansaxena.paymentswitch.iso8583.Fields;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Exception;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Message;
import com.aryansaxena.paymentswitch.repository.TransactionRepository;

class AuthorizationServiceTest {

  private AccountService accountService;
  private TransactionRepository repository;
  private AuthorizationService service;

  @BeforeEach
  void setUp() {
    accountService = new AccountService();
    repository = Mockito.mock(TransactionRepository.class);
    when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    service = new AuthorizationService(new LuhnValidator(), accountService, repository, 500_000L);
  }

  private Iso8583Message request(String pan, String amount) {
    Iso8583Message m = new Iso8583Message("0100");
    m.set(Fields.PAN, pan);
    m.set(Fields.AMOUNT, amount);
    m.set(Fields.STAN, "000001");
    m.set(Fields.CURRENCY, "356");
    return m;
  }

  @Test
  void approvesValidTransactionAndReturnsAuthId() {
    Iso8583Message response = service.authorize(request("4111111111111111", "000000010000"));

    assertThat(response.getMti()).isEqualTo("0110");
    assertThat(response.get(Fields.RESPONSE_CODE)).isEqualTo(ResponseCode.APPROVED);
    assertThat(response.get(Fields.AUTH_ID)).isNotBlank();
  }

  @Test
  void declinesCardThatFailsLuhn() {
    Iso8583Message response = service.authorize(request("4111111111111112", "000000010000"));

    assertThat(response.get(Fields.RESPONSE_CODE)).isEqualTo(ResponseCode.INVALID_CARD);
  }

  @Test
  void declinesAmountAbovePerTransactionLimit() {
    Iso8583Message response = service.authorize(request("4111111111111111", "000000600000"));

    assertThat(response.get(Fields.RESPONSE_CODE)).isEqualTo(ResponseCode.EXCEEDS_LIMIT);
  }

  @Test
  void declinesBlockedCard() {
    accountService.block("4111111111111111");

    Iso8583Message response = service.authorize(request("4111111111111111", "000000010000"));

    assertThat(response.get(Fields.RESPONSE_CODE)).isEqualTo(ResponseCode.DO_NOT_HONOR);
  }

  @Test
  void declinesWhenBalanceIsInsufficient() {
    // Default balance is 1,000,000 minor units; two approved 400,000 debits leave 200,000.
    service.authorize(request("4111111111111111", "000000400000"));
    service.authorize(request("4111111111111111", "000000400000"));

    Iso8583Message response = service.authorize(request("4111111111111111", "000000400000"));

    assertThat(response.get(Fields.RESPONSE_CODE)).isEqualTo(ResponseCode.INSUFFICIENT_FUNDS);
  }

  @Test
  void rejectsNonAuthorizationMti() {
    Iso8583Message message = request("4111111111111111", "000000010000");
    message.setMti("0200");

    assertThatThrownBy(() -> service.authorize(message)).isInstanceOf(Iso8583Exception.class);
  }
}
