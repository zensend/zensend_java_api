[![Build Status](https://travis-ci.org/zensend/zensend_java_api.svg?branch=master)](https://travis-ci.org/zensend/zensend_java_api)

## Installation

The library is distributed via Maven [io.zensend::zensend](http://mvnrepository.com/artifact/io.zensend/zensend/0.0.2)

## Examples
Create an instance of the client
```java
Client client = new Client("YOUR-API-KEY");
```
### Sending SMS
To send an SMS, you must specify the originator, body and numbers:
```java
Message message = new Message();
message.originator = "orig";
message.body = "message body";
message.numbers = new String[]{"447777777777", "448888888888"};

SmsResult smsResult = client.sendSms(message);

System.out.println(
    smsResult.txGuid + ":" +
    smsResult.numbers + ":" +
    smsResult.smsParts + ":" +
    smsResult.encoding + ":" +
    smsResult.costInPence + ":" +
    smsResult.newBalanceInPence
);
```

You can also specify the following optional params:

```java
Message message = new Message();
message.originator = "orig";
message.body = "message body";
message.numbers = new String[]{"447777777777", "448888888888"};
message.originatorType = Message.OriginatorType.ALPHA; // either ALPHA or MSISDN
message.timeToLiveInMinutes = 100;
message.smsEncoding = Message.SmsEncoding.GSM; // either GSM or UCS2

SmsResult smsResult = client.sendSms(message);

System.out.println(
    smsResult.txGuid + ":" +
    smsResult.numbers + ":" +
    smsResult.smsParts + ":" +
    smsResult.encoding + ":" +
    smsResult.costInPence + ":" +
    smsResult.newBalanceInPence
);
```

### Checking your balance
This will return your current balance:
```java
BigDecimal balance = client.checkBalance();

System.out.println(balance);
```

### Listing prices
This will return a HashMap with all our prices by country code:
```java
HashMap<String, BigDecimal> prices = client.getPrices();

System.out.println(prices);
```

### Operator Lookup
This allows you to lookup the operator of a given MSISDN
```java
OperatorLookupResult opLookup = client.lookupOperator("447821425039");

System.out.println(
    opLookup.mcc + ":" +
    opLookup.mnc + ":" +
    opLookup.operator + ":" +
    opLookup.costInPence + ":" +
    opLookup.newBalanceInPence
);
```

## Release

    mvn release:clean release:prepare
    mvn release:perform -P release-profile
