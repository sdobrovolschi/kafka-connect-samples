package com.example.kafkaconnect;

import java.math.BigDecimal;

import static java.math.BigDecimal.TEN;

public record Money(BigDecimal amount, String currencyCode) {

    public static final Money TEN_EUROS = new Money(TEN, "EUR");
}
