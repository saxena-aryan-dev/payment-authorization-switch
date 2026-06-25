package com.aryansaxena.paymentswitch.iso8583;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory representation of an ISO 8583 message: a message type indicator (MTI) plus an ordered
 * map of data elements keyed by field number.
 */
public class Iso8583Message {

  private String mti;
  private final Map<Integer, String> fields = new TreeMap<>();

  public Iso8583Message() {}

  public Iso8583Message(String mti) {
    this.mti = mti;
  }

  public String getMti() {
    return mti;
  }

  public void setMti(String mti) {
    this.mti = mti;
  }

  public Iso8583Message set(int field, String value) {
    fields.put(field, value);
    return this;
  }

  public boolean has(int field) {
    return fields.containsKey(field);
  }

  public String get(int field) {
    return fields.get(field);
  }

  public Optional<String> find(int field) {
    return Optional.ofNullable(fields.get(field));
  }

  public Map<Integer, String> getFields() {
    return fields;
  }
}
