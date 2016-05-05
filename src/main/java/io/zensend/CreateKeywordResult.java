package io.zensend;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateKeywordResult {

    @JsonProperty("cost_in_pence")
    public BigDecimal costInPence;

    @JsonProperty("new_balance_in_pence")
    public BigDecimal newBalanceInPence;
}