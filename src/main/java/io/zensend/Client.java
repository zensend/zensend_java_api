package io.zensend;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;

public class Client {
    private String apiKey;
    private String url;
    private static final String ZENSEND_URL = "https://zensend.io";

    public Client(String apiKey) {
        this(apiKey, ZENSEND_URL);
    }

    public Client(String apiKey, String url) {
        this.apiKey = apiKey;
        this.url = url;
    }

    public SmsResult sendSms(Message message) throws JsonParseException, JsonMappingException, ZenSendException, IOException, UnirestException {
        assertNoCommas(message.numbers);

        MultipartBody query = Unirest.post(this.url + "/v3/sendsms")
            .header("X-API-KEY", this.apiKey)
            .field("BODY", message.body)
            .field("NUMBERS", StringUtils.join(message.numbers, ","))
            .field("ORIGINATOR", message.originator);

        if (message.originatorType != null) {
            query = query.field("ORIGINATOR_TYPE", message.originatorType.name().toLowerCase());
        }

        if (message.timeToLiveInMinutes != null) {
            query = query.field("TIMETOLIVE", message.timeToLiveInMinutes);
        }

        if (message.smsEncoding != null) {
            query = query.field("ENCODING", message.smsEncoding.name().toLowerCase());
        }

        return handleResponse(query.asString(), new TypeReference<Result<SmsResult>>(){});
    }

    public OperatorLookupResult lookupOperator(String number) throws UnirestException, JsonParseException, JsonMappingException, ZenSendException, IOException {
        HttpResponse<String> response = Unirest.get(this.url + "/v3/operator_lookup")
            .header("X-API-KEY", this.apiKey)
            .queryString("NUMBER", number)
            .asString();

        return handleResponse(response, new TypeReference<Result<OperatorLookupResult>>(){});
    }

    public HashMap<String, BigDecimal> getPrices() throws UnirestException, ZenSendException, JsonParseException, JsonMappingException, IOException {
        HttpResponse<String> response = Unirest.get(this.url + "/v3/prices")
            .header("X-API-KEY", this.apiKey)
            .asString();

        return handleResponse(response, new TypeReference<Result<Prices>>(){}).pricesInPence;
    }

    public BigDecimal checkBalance() throws UnirestException, ZenSendException, JsonParseException, JsonMappingException, IOException {
        HttpResponse<String> response = Unirest.get(this.url + "/v3/checkbalance")
                                                 .header("X-API-KEY", this.apiKey)
                                                 .asString();

        return handleResponse(response, new TypeReference<Result<Balance>>(){}).balance;
    }

    private void assertNoCommas(String[] numbers) {
        for (String number : numbers) {
            if (number.contains(",")) {
                throw new IllegalArgumentException("Comma not allowed in numbers");
            }
        }
    }

    private <T> T handleResponse(HttpResponse<String> response, TypeReference<Result<T>> typeRef) throws ZenSendException, JsonParseException, JsonMappingException, IOException {
        if (!response.getHeaders().getFirst("content-type").equals("application/json")) {
            throw new ZenSendException(response.getStatus(), null, null, null, null);
        }

        ObjectMapper mapper = new ObjectMapper();

        Result<T> result = mapper.readValue(response.getBody(), typeRef);

        if (result.success == null) {
            handleError(response.getStatus(), result.failure);
        }

        return result.success;
    }

    private void handleError(int httpCode, ZenSendError error) throws ZenSendException {
        if (error != null) {
            throw new ZenSendException(
                httpCode,
                error.failCode,
                error.parameter,
                error.costInPence,
                error.newBalanceInPence);
        } else {
            throw new ZenSendException(httpCode, null, null, null, null);
        }
    }

    // helper classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ZenSendError {
        @JsonProperty("failcode")
        public String failCode;

        public String parameter;

        @JsonProperty("cost_in_pence")
        public BigDecimal costInPence;

        @JsonProperty("new_balance_in_pence")
        public BigDecimal newBalanceInPence;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Result<T> {
        public T success;
        public ZenSendError failure;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Balance {
        public BigDecimal balance;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Prices {
        @JsonProperty("prices_in_pence")
        public HashMap<String, BigDecimal> pricesInPence;
    }
}
