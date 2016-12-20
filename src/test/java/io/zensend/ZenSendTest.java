package io.zensend;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ZenSendTest {

    private static final String apiKey = "API-KEY-123";
    private static final String host = "http://localhost:8089";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private Client client;
    
    @Before
    public void setup() {
        client = new Client(apiKey, HttpClients.createDefault(), host, host);
    }
    
    @After
    public void teardown() throws Exception {
        client.close();
    }
    
    @Test
    public void checkBalanceSuccessTest() throws Exception {
        setupMockServer("/v3/checkbalance", 200, "{\"success\":{\"balance\":4000.84}}");


        BigDecimal result = client.checkBalance();

        assertEquals(new BigDecimal("4000.84"), result);
    }


    
    @Test
    public void checkBalanceFailureTest() throws Exception {
        setupMockServer("/v3/checkbalance", 403, "{\"failure\":{\"failcode\":\"NOT_AUTHORIZED\"}}");


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


        HashMap<String, BigDecimal> result = client.getPrices();
        assertEquals(expectedPrices, result);
    }

    @Test
    public void getPricesFailureTest() throws Exception {
        setupMockServer("/v3/prices", 403, "{\"failure\":{\"failcode\":\"NOT_AUTHORIZED\"}}");


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
        message.body = "message body£";
        message.originator = "orig";

        stubFor(post(urlPathEqualTo("/v3/sendsms"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":{\"txguid\":\"eb224587-840e-456c-9e36-7e8af1fe0d56\",\"numbers\":2,\"smsparts\":1,\"encoding\":\"gsm\",\"cost_in_pence\":0.04,\"new_balance_in_pence\":3985.8}}")));

        SmsResult result = client.sendSms(message);

        assertEquals("eb224587-840e-456c-9e36-7e8af1fe0d56", result.txGuid);
        assertEquals(2, result.numbers);
        assertEquals(1, result.smsParts);
        assertEquals("gsm", result.encoding);
        assertEquals(new BigDecimal("0.04"), result.costInPence);
        assertEquals(new BigDecimal("3985.8"), result.newBalanceInPence);
        
        verify(postRequestedFor(urlPathEqualTo("/v3/sendsms"))
            .withRequestBody(equalTo("BODY=message+body%C2%A3&NUMBERS=44787878787%2C449999999999&ORIGINATOR=orig")));
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

        SmsResult result = client.sendSms(message);

        assertEquals("eb224587-840e-456c-9e36-7e8af1fe0d56", result.txGuid);
        assertEquals(2, result.numbers);
        assertEquals(1, result.smsParts);
        assertEquals("gsm", result.encoding);
        assertEquals(new BigDecimal("0.04"), result.costInPence);
        assertEquals(new BigDecimal("3985.8"), result.newBalanceInPence);
        
        verify(postRequestedFor(urlPathEqualTo("/v3/sendsms"))
            .withRequestBody(equalTo("BODY=message+body&NUMBERS=44787878787%2C449999999999&ORIGINATOR=orig&ORIGINATOR_TYPE=alpha&TIMETOLIVE=100&ENCODING=gsm")));
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


        try {
            client.checkBalance();
            fail();
        } catch (ZenSendException ex) {
            assertEquals(500, ex.httpCode);
        }
    }

    @Test
    public void createMsisdnVerificationTest() throws Exception {
        stubFor(post(urlPathEqualTo("/api/msisdn_verify"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\": {\"session\":\"SESS\"}}")));

        String sess = client.createMsisdnVerification("441234567890");

        assertEquals("SESS", sess);
        verify(postRequestedFor(urlPathEqualTo("/api/msisdn_verify"))
            .withRequestBody(equalTo("NUMBER=441234567890")));

    }  

    @Test
    public void createMsisdnVerificationWithOptionsTest() throws Exception {

        stubFor(post(urlPathEqualTo("/api/msisdn_verify"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\": {\"session\":\"SESS\"}}")));

        String sess = client.createMsisdnVerification("441234567890", "message", "originator");

        assertEquals("SESS", sess);
        verify(postRequestedFor(urlPathEqualTo("/api/msisdn_verify"))
            .withRequestBody(equalTo("NUMBER=441234567890&MESSAGE=message&ORIGINATOR=originator")));

    }

    @Test
    public void msisdnVerificationStatusTest() throws Exception {
        stubFor(get(urlPathEqualTo("/api/msisdn_verify"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .withQueryParam("SESSION", equalTo("SESS"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\": {\"msisdn\":\"441234567890\"}}")));

        String msisdn = client.msisdnVerificationStatus("SESS");
        assertEquals("441234567890", msisdn);

    }


    @Test
    public void testCreateSubAccount() throws Exception {
        Message message = new Message();
        message.numbers = new String[]{"44787878787", "449999999999"};
        message.body = "message body£";
        message.originator = "orig";

        stubFor(post(urlPathEqualTo("/v3/sub_accounts"))
            .withHeader("X-API-KEY", equalTo(apiKey))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":{\"name\":\"Name\",\"api_key\":\"ApiKey\"}}")));

        CreateSubAccountResult result = client.createSubAccount("Name");

        assertEquals("Name", result.name);
        assertEquals("ApiKey", result.apiKey);
       
        
        verify(postRequestedFor(urlPathEqualTo("/v3/sub_accounts"))
            .withRequestBody(equalTo("NAME=Name")));
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

