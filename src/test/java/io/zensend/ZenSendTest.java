package io.zensend;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ZenSendTest {

    private static final String apiKey = "API-KEY-123";
    private static final String host = "http://localhost:8089";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void checkBalanceSuccessTest() throws Exception {
        setupMockServer("/v3/checkbalance", 200, "{\"success\":{\"balance\":4000.84}}");

        Client client = getClient();

        BigDecimal result = client.checkBalance();

        assertEquals(new BigDecimal("4000.84"), result);
    }

    private Client getClient() {
        return new Client(apiKey, host);
    }
    
    @Test
    public void checkBalanceFailureTest() throws Exception {
        setupMockServer("/v3/checkbalance", 403, "{\"failure\":{\"failcode\":\"NOT_AUTHORIZED\"}}");

        Client client = getClient();

        try {
            client.checkBalance();
            fail();
        } catch (ZenSendException ex) {
            assertEquals(403, ex.httpCode);
            assertEquals("NOT_AUTHORIZED", ex.failCode);
        }
    }
    
    @Test
    public void operatorLookupSuccessTest() throws Exception {
        String number = "447777777777";

        stubFor(get(urlPathEqualTo("/v3/operator_lookup"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .withQueryParam("NUMBER", equalTo(number))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":{\"mcc\":\"234\",\"mnc\":\"34\",\"operator\":\"eeora-uk\",\"new_balance_in_pence\":3985.84,\"cost_in_pence\":15.0}}")));

        Client client = getClient();

        OperatorLookupResult result = client.lookupOperator(number);

        assertEquals("234", result.mcc);
        assertEquals("34", result.mnc);
        assertEquals("eeora-uk", result.operator);
        assertEquals(new BigDecimal("3985.84"), result.newBalanceInPence);
        assertEquals(new BigDecimal("15.0"), result.costInPence);
    }

    @Test
    public void operatorLookupSystemFailureTest() throws Exception {
        String number = "557777777777";

        stubFor(get(urlPathEqualTo("/v3/operator_lookup"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .withQueryParam("NUMBER", equalTo(number))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"failure\":{\"failcode\":\"SYSTEM_FAILURE\",\"new_balance_in_pence\":4046.0,\"cost_in_pence\":15.0}}")));

        Client client = getClient();

        try {
            client.lookupOperator(number);
            fail();
        } catch (ZenSendException ex) {
            assertEquals(503, ex.httpCode);
            assertEquals("SYSTEM_FAILURE", ex.failCode);
            assertEquals(new BigDecimal("4046.0"), ex.newBalanceInPence);
            assertEquals(new BigDecimal("15.0"), ex.costInPence);
        }
    }

    @Test
    public void getPricesSuccessTest() throws Exception {
        HashMap<String, BigDecimal> expectedPrices = new HashMap<String, BigDecimal>();
        expectedPrices.put("NL", new BigDecimal("0.04"));
        expectedPrices.put("LR", new BigDecimal("0.028"));

        setupMockServer("/v3/prices", 200, "{\"success\":{\"prices_in_pence\":{\"NL\":0.04,\"LR\":0.028}}}");

        Client client = getClient();

        HashMap<String, BigDecimal> result = client.getPrices();
        assertEquals(expectedPrices, result);
    }

    @Test
    public void getPricesFailureTest() throws Exception {
        setupMockServer("/v3/prices", 403, "{\"failure\":{\"failcode\":\"NOT_AUTHORIZED\"}}");

        Client client = getClient();

        try {
            client.getPrices();
            fail();
        } catch (ZenSendException ex) {
            assertEquals(403, ex.httpCode);
            assertEquals("NOT_AUTHORIZED", ex.failCode);
        }
    }

    @Test
    public void sendSmsMinimalSuccessTest() throws Exception {
        Message message = new Message();
        message.numbers = new String[]{"44787878787", "449999999999"};
        message.body = "message body";
        message.originator = "orig";

        stubFor(post(urlPathEqualTo("/v3/sendsms"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":{\"txguid\":\"eb224587-840e-456c-9e36-7e8af1fe0d56\",\"numbers\":2,\"smsparts\":1,\"encoding\":\"gsm\",\"cost_in_pence\":0.04,\"new_balance_in_pence\":3985.8}}")));

        Client client = getClient();
        SmsResult result = client.sendSms(message);

        assertEquals("eb224587-840e-456c-9e36-7e8af1fe0d56", result.txGuid);
        assertEquals(2, result.numbers);
        assertEquals(1, result.smsParts);
        assertEquals("gsm", result.encoding);
        assertEquals(new BigDecimal("0.04"), result.costInPence);
        assertEquals(new BigDecimal("3985.8"), result.newBalanceInPence);
        
        verify(postRequestedFor(urlPathEqualTo("/v3/sendsms"))
            .withRequestBody(equalTo("BODY=message+body&NUMBERS=44787878787%2C449999999999&ORIGINATOR=orig")));
    }

    @Test
    public void sendSmsOptionalParamsSuccessTest() throws Exception {
        Message message = new Message();
        message.numbers = new String[]{"44787878787", "449999999999"};
        message.body = "message body";
        message.originator = "orig";
        message.originatorType = Message.OriginatorType.ALPHA;
        message.smsEncoding = Message.SmsEncoding.GSM;
        message.timeToLiveInMinutes = 100;

        stubFor(post(urlPathEqualTo("/v3/sendsms"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":{\"txguid\":\"eb224587-840e-456c-9e36-7e8af1fe0d56\",\"numbers\":2,\"smsparts\":1,\"encoding\":\"gsm\",\"cost_in_pence\":0.04,\"new_balance_in_pence\":3985.8}}")));

        Client client = getClient();
        SmsResult result = client.sendSms(message);

        assertEquals("eb224587-840e-456c-9e36-7e8af1fe0d56", result.txGuid);
        assertEquals(2, result.numbers);
        assertEquals(1, result.smsParts);
        assertEquals("gsm", result.encoding);
        assertEquals(new BigDecimal("0.04"), result.costInPence);
        assertEquals(new BigDecimal("3985.8"), result.newBalanceInPence);
        
        verify(postRequestedFor(urlPathEqualTo("/v3/sendsms"))
            .withRequestBody(equalTo("BODY=message+body&ENCODING=gsm&NUMBERS=44787878787%2C449999999999&ORIGINATOR=orig&ORIGINATOR_TYPE=alpha&TIMETOLIVE=100")));
    }

    @Test
    public void sendSmsFailureTest() throws Exception {
        Message message = new Message();
        message.numbers = new String[]{"44787878787", "449999999999"};
        message.body = "message body";
        message.originator = "orig";

        stubFor(post(urlPathEqualTo("/v3/sendsms"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"failure\":{\"failcode\":\"NOT_AUTHORIZED\"}}")));

        Client client = getClient();

        try {
            client.sendSms(message);
            fail();
        } catch (ZenSendException ex) {
            assertEquals(403, ex.httpCode);
            assertEquals("NOT_AUTHORIZED", ex.failCode);
        }
    }

    @Test
    public void sendSmsInvalidNumbersTest() throws Exception {
        Message message = new Message();
        message.numbers = new String[]{"4478,7878787", "449999999999"};
        message.body = "message body";
        message.originator = "orig";

        Client client = getClient();

        try {
            client.sendSms(message);
            fail();
        } catch (IllegalArgumentException ex) {

        }
    }
    
    @Test
    public void invalidResponseTypeTest() throws Exception {
        stubFor(get(urlPathEqualTo("/v3/checkbalance"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/html")
                .withBody("<body>hello</body>")));

        Client client = getClient();

        try {
            client.checkBalance();
            fail();
        } catch (ZenSendException ex) {
            assertEquals(500, ex.httpCode);
        }
    }

    private void setupMockServer(String url, int status, String body) {
        stubFor(get(urlEqualTo(url))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }
}

