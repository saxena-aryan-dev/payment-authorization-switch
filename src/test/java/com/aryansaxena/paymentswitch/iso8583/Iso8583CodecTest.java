package com.aryansaxena.paymentswitch.iso8583;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Iso8583CodecTest {

  private final Iso8583Codec codec = new Iso8583Codec();

  @Test
  void encodesAndDecodesRoundTrip() {
    Iso8583Message message = new Iso8583Message("0100");
    message.set(Fields.PAN, "4111111111111111");
    message.set(Fields.PROCESSING_CODE, "000000");
    message.set(Fields.AMOUNT, "000000010000");
    message.set(Fields.STAN, "000123");
    message.set(Fields.CURRENCY, "356");

    String encoded = codec.encode(message);
    Iso8583Message decoded = codec.decode(encoded);

    assertThat(decoded.getMti()).isEqualTo("0100");
    assertThat(decoded.get(Fields.PAN)).isEqualTo("4111111111111111");
    assertThat(decoded.get(Fields.PROCESSING_CODE)).isEqualTo("000000");
    assertThat(decoded.get(Fields.AMOUNT)).isEqualTo("000000010000");
    assertThat(decoded.get(Fields.STAN)).isEqualTo("000123");
    assertThat(decoded.get(Fields.CURRENCY)).isEqualTo("356");
  }

  @Test
  void encodesLlvarLengthPrefixForPan() {
    Iso8583Message message = new Iso8583Message("0100");
    message.set(Fields.PAN, "4111111111111111");

    String encoded = codec.encode(message);

    // MTI(4) + bitmap(16), then field 2 (LLVAR) begins with its 2-digit length prefix.
    assertThat(encoded.substring(20, 22)).isEqualTo("16");
  }

  @Test
  void rejectsMessageShorterThanHeader() {
    assertThatThrownBy(() -> codec.decode("0100")).isInstanceOf(Iso8583Exception.class);
  }

  @Test
  void rejectsFixedFieldWithWrongLength() {
    Iso8583Message message = new Iso8583Message("0100");
    message.set(Fields.CURRENCY, "35"); // currency must be exactly 3 digits

    assertThatThrownBy(() -> codec.encode(message)).isInstanceOf(Iso8583Exception.class);
  }

  @Test
  void rejectsMissingMti() {
    Iso8583Message message = new Iso8583Message();
    message.set(Fields.STAN, "000001");

    assertThatThrownBy(() -> codec.encode(message)).isInstanceOf(Iso8583Exception.class);
  }
}
