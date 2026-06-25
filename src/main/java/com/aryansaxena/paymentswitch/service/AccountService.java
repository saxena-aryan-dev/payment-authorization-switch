package com.aryansaxena.paymentswitch.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Minimal in-memory cardholder ledger that stands in for an issuer host. Unknown PANs are seeded
 * with a default available balance; approved authorizations debit that balance.
 */
@Component
public class AccountService {

  private static final long DEFAULT_BALANCE_MINOR_UNITS = 1_000_000L;

  private final Map<String, Long> balances = new ConcurrentHashMap<>();
  private final Set<String> blockedPans = ConcurrentHashMap.newKeySet();

  public long balanceOf(String pan) {
    return balances.computeIfAbsent(pan, key -> DEFAULT_BALANCE_MINOR_UNITS);
  }

  public boolean isBlocked(String pan) {
    return blockedPans.contains(pan);
  }

  public void block(String pan) {
    blockedPans.add(pan);
  }

  public void debit(String pan, long amountMinorUnits) {
    balances.merge(pan, -amountMinorUnits, Long::sum);
  }
}
