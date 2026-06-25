package com.aryansaxena.paymentswitch.iso8583;

/**
 * Definition of a single ISO 8583 data element: its number, how its length is encoded, the maximum
 * (or exact) length and a human-readable label.
 */
public record FieldSpec(int field, FieldType type, int length, String label) {}
