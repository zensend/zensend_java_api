package io.zensend;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSubAccountResult {
    public String name;

    @JsonProperty("api_key")
    public String apiKey;


}
