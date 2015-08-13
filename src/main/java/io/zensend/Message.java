package io.zensend;

public class Message {
    public static enum OriginatorType {
        ALPHA,
        MSISDN
    }

    public static enum SmsEncoding {
        GSM,
        UCS2
    }

    public String originator;
    public String body;
    public String[] numbers;
    public OriginatorType originatorType;
    public Integer timeToLiveInMinutes;
    public SmsEncoding smsEncoding;
}
