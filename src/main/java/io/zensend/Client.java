package io.zensend;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Client implements Closeable {
    private String apiKey;
    private String url;
    private String verifyUrl;
    private static final String ZENSEND_URL = "https://api.zensend.io";
    private static final String VERIFY_URL = "https://verify.zensend.io";

    private CloseableHttpClient client;
    
    public Client(String apiKey) {
        this(apiKey, HttpClients.custom()
                .setKeepAliveStrategy(new FixedKeepAliveStrategy(5 * 1000))
                .build(), ZENSEND_URL);
    }

    public Client(String apiKey, CloseableHttpClient client) {
        this(apiKey, client, ZENSEND_URL, VERIFY_URL);
    }
    
    public Client(String apiKey, CloseableHttpClient client, String url) {
        this(apiKey, client, url, VERIFY_URL);
    }

    public Client(String apiKey, CloseableHttpClient client, String url, String verifyUrl) {
        this.apiKey = apiKey;
        this.url = url;
        this.client = client;
        this.verifyUrl = verifyUrl;
    }

    public void close() throws IOException {
        client.close();
    }

    public String createMsisdnVerification(String number) throws ZenSendException, IOException {
        return createMsisdnVerification(number, null, null);
    }

    public String createMsisdnVerification(String number, String message, String originator) throws ZenSendException, IOException {
        Form form = Form.form();
        form.add("NUMBER", number);


        if (message != null) {
            form.add("MESSAGE", message);
        }

        if (originator != null) {
            form.add("ORIGINATOR", originator);
        }
    
        HttpPost post = new HttpPost(this.verifyUrl + "/api/msisdn_verify");
        post.setEntity(new UrlEncodedFormEntity(form.build(), "utf-8"));

        return handleHttpResponse(post, new TypeReference<Result<CreateMsisdnVerificationResult>>(){}).session;
    }

    public String msisdnVerificationStatus(String session) throws ZenSendException, IOException {
        URI uri;
        
        try {
            uri = new URIBuilder(this.verifyUrl + "/api/msisdn_verify").addParameter("SESSION", session).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        
        HttpGet get = new HttpGet(uri);
        
        return handleHttpResponse(get,
            new TypeReference<Result<MsisdnVerificationStatusResult>>(){}).msisdn;

    }

    public SmsResult sendSms(Message message) throws ZenSendException, IOException {
        assertNoCommas(message.numbers);

        Form form = Form.form()
                .add("BODY", message.body)
                .add("NUMBERS", StringUtils.join(message.numbers, ","))
                .add("ORIGINATOR", message.originator);
                
        if (message.originatorType != null) {
            form = form.add("ORIGINATOR_TYPE", message.originatorType.name().toLowerCase());
        }

        if (message.timeToLiveInMinutes != null) {
            form = form.add("TIMETOLIVE", message.timeToLiveInMinutes.toString());
        }

        if (message.smsEncoding != null) {
            form = form.add("ENCODING", message.smsEncoding.name().toLowerCase());
        }

        HttpPost post = new HttpPost(this.url + "/v3/sendsms");
        post.setEntity(new UrlEncodedFormEntity(form.build(), "utf-8"));
        
        return handleHttpResponse(post, new TypeReference<Result<SmsResult>>(){});


        
    }

    public OperatorLookupResult lookupOperator(String number) throws ZenSendException, IOException {
        
        URI uri;
        
        try {
            uri = new URIBuilder(this.url + "/v3/operator_lookup").addParameter("NUMBER", number).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        
        HttpGet get = new HttpGet(uri);
        
        return handleHttpResponse(get,
            new TypeReference<Result<OperatorLookupResult>>(){});

    }

    public HashMap<String, BigDecimal> getPrices() throws  ZenSendException, IOException {
        
        HttpGet get = new HttpGet(this.url + "/v3/prices");
        
        return handleHttpResponse(get,
            new TypeReference<Result<Prices>>(){}).pricesInPence;
    }

    public BigDecimal checkBalance() throws ZenSendException, IOException {
        
        HttpGet get = new HttpGet(this.url + "/v3/checkbalance");

        return handleHttpResponse(get,
            new TypeReference<Result<Balance>>(){}).balance;
        

    }

    private void assertNoCommas(String[] numbers) {
        for (String number : numbers) {
            if (number.contains(",")) {
                throw new IllegalArgumentException("Comma not allowed in numbers");
            }
        }
    }

    private <T> T handleHttpResponse(HttpRequestBase request, final TypeReference<Result<T>> typeRef) throws ZenSendException, IOException {
        
        request.addHeader("X-API-KEY", this.apiKey);

        CloseableHttpResponse response = client.execute(request);
        try {
            Header contentType = response.getFirstHeader("content-type");
            
            if (!(contentType != null && "application/json".equals(contentType.getValue()))) {
                EntityUtils.consume(response.getEntity());
                throw new ZenSendException(response.getStatusLine().getStatusCode(), null, null, null, null);
            }

            ObjectMapper mapper = new ObjectMapper();
             
            Result<T> result = mapper.readValue(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")), typeRef);

            if (result.success == null) {
                handleError(response.getStatusLine().getStatusCode(), result.failure);
            }

            return result.success;            
        } finally {
            response.close();
        }
      

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CreateMsisdnVerificationResult {
        public String session;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MsisdnVerificationStatusResult {
        public String msisdn;
    }
}
