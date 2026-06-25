package com.aryansaxena.paymentswitch.iso8583;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Encodes and decodes ISO 8583 messages using ASCII representation and a single 64-bit primary
 * bitmap. Supports FIXED, LLVAR and LLLVAR data elements as declared in {@link Fields}.
 */
@Component
public class Iso8583Codec {

  private static final int MTI_LENGTH = 4;
  private static final int BITMAP_HEX_LENGTH = 16;
  private static final int BITMAP_BITS = 64;

  public String encode(Iso8583Message message) {
    if (message.getMti() == null || message.getMti().length() != MTI_LENGTH) {
      throw new Iso8583Exception("MTI must be exactly 4 digits");
    }
    StringBuilder body = new StringBuilder();
    long bitmap = 0L;
    for (Map.Entry<Integer, String> entry : message.getFields().entrySet()) {
      int field = entry.getKey();
      if (field < 2 || field > BITMAP_BITS) {
        throw new Iso8583Exception("Field out of supported range: " + field);
      }
      bitmap |= (1L << (BITMAP_BITS - field));
      body.append(encodeField(field, entry.getValue()));
    }
    String bitmapHex = String.format("%016X", bitmap);
    return message.getMti() + bitmapHex + body;
  }

  private String encodeField(int field, String value) {
    FieldSpec spec = Fields.spec(field);
    return switch (spec.type()) {
      case FIXED -> {
        if (value.length() != spec.length()) {
          throw new Iso8583Exception(
              "Field " + field + " must be " + spec.length() + " chars, was " + value.length());
        }
        yield value;
      }
      case LLVAR -> {
        if (value.length() > spec.length()) {
          throw new Iso8583Exception("Field " + field + " exceeds max length " + spec.length());
        }
        yield String.format("%02d", value.length()) + value;
      }
      case LLLVAR -> {
        if (value.length() > spec.length()) {
          throw new Iso8583Exception("Field " + field + " exceeds max length " + spec.length());
        }
        yield String.format("%03d", value.length()) + value;
      }
    };
  }

  public Iso8583Message decode(String raw) {
    if (raw == null || raw.length() < MTI_LENGTH + BITMAP_HEX_LENGTH) {
      throw new Iso8583Exception("Message too short to contain MTI and bitmap");
    }
    Iso8583Message message = new Iso8583Message(raw.substring(0, MTI_LENGTH));
    String bitmapHex = raw.substring(MTI_LENGTH, MTI_LENGTH + BITMAP_HEX_LENGTH);
    long bitmap;
    try {
      bitmap = Long.parseUnsignedLong(bitmapHex, 16);
    } catch (NumberFormatException e) {
      throw new Iso8583Exception("Invalid bitmap: " + bitmapHex);
    }
    if ((bitmap & (1L << (BITMAP_BITS - 1))) != 0) {
      throw new Iso8583Exception("Secondary bitmap (field 1) is not supported");
    }
    int pos = MTI_LENGTH + BITMAP_HEX_LENGTH;
    for (int field = 2; field <= BITMAP_BITS; field++) {
      boolean present = (bitmap & (1L << (BITMAP_BITS - field))) != 0;
      if (!present) {
        continue;
      }
      FieldSpec spec = Fields.spec(field);
      pos = readField(raw, pos, field, spec, message);
    }
    return message;
  }

  private int readField(String raw, int pos, int field, FieldSpec spec, Iso8583Message message) {
    int prefix =
        switch (spec.type()) {
          case FIXED -> 0;
          case LLVAR -> 2;
          case LLLVAR -> 3;
        };
    int length;
    if (prefix == 0) {
      length = spec.length();
    } else {
      requireLength(raw, pos, prefix, field);
      length = parseInt(raw.substring(pos, pos + prefix), field);
      pos += prefix;
      if (length > spec.length()) {
        throw new Iso8583Exception("Field " + field + " length " + length + " exceeds max");
      }
    }
    requireLength(raw, pos, length, field);
    message.set(field, raw.substring(pos, pos + length));
    return pos + length;
  }

  private void requireLength(String raw, int pos, int length, int field) {
    if (pos + length > raw.length()) {
      throw new Iso8583Exception("Truncated message while reading field " + field);
    }
  }

  private int parseInt(String value, int field) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new Iso8583Exception("Invalid length indicator for field " + field + ": " + value);
    }
  }
}
