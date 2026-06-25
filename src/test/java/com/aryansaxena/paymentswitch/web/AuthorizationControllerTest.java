package com.aryansaxena.paymentswitch.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.aryansaxena.paymentswitch.iso8583.Fields;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Codec;
import com.aryansaxena.paymentswitch.iso8583.Iso8583Message;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private Iso8583Codec codec;

  private String authRequest(String pan, String amount, String stan) {
    Iso8583Message m = new Iso8583Message("0100");
    m.set(Fields.PAN, pan);
    m.set(Fields.PROCESSING_CODE, "000000");
    m.set(Fields.AMOUNT, amount);
    m.set(Fields.STAN, stan);
    m.set(Fields.CURRENCY, "356");
    return codec.encode(m);
  }

  @Test
  void approvesAndPersistsTransaction() throws Exception {
    String message = authRequest("4111111111111111", "000000010000", "000777");

    mockMvc
        .perform(post("/api/v1/authorizations").contentType(MediaType.TEXT_PLAIN).content(message))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responseCode").value("00"))
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.maskedPan").value("************1111"))
        .andExpect(jsonPath("$.approvalCode").isNotEmpty());

    mockMvc
        .perform(get("/api/v1/transactions/000777"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responseCode").value("00"))
        .andExpect(jsonPath("$.maskedPan").value("************1111"));
  }

  @Test
  void declinesInvalidCard() throws Exception {
    String message = authRequest("4111111111111112", "000000010000", "000888");

    mockMvc
        .perform(post("/api/v1/authorizations").contentType(MediaType.TEXT_PLAIN).content(message))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responseCode").value("14"))
        .andExpect(jsonPath("$.status").value("DECLINED"));
  }

  @Test
  void rejectsMalformedMessageWithBadRequest() throws Exception {
    mockMvc
        .perform(post("/api/v1/authorizations").contentType(MediaType.TEXT_PLAIN).content("0100"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnsNotFoundForUnknownStan() throws Exception {
    mockMvc.perform(get("/api/v1/transactions/999999")).andExpect(status().isNotFound());
  }
}
