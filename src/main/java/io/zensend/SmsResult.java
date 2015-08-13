package io.zensend;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SmsResult {
    @JsonProperty("txguid")
    public String txGuid;

    public int numbers;

    @JsonProperty("smsparts")
    public int smsParts;

    public String encoding;

    @JsonProperty("cost_in_pence")
    public BigDecimal costInPence;

    @JsonProperty("new_balance_in_pence")
    public BigDecimal newBalanceInPence;
}