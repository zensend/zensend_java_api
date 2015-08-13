package io.zensend;

import java.math.BigDecimal;

public class ZenSendException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public int httpCode;
    public String failCode;
    public String parameter;
    public BigDecimal costInPence;
    public BigDecimal newBalanceInPence;

    public ZenSendException(int httpCode, String failCode, String parameter, BigDecimal costInPence, BigDecimal newBalanceInPence) {
        this.httpCode = httpCode;
        this.failCode = failCode;
        this.parameter = parameter;
        this.costInPence = costInPence;
        this.newBalanceInPence = newBalanceInPence;
    }

    public String getMessage() {
        return "HTTP Code: " + this.httpCode + ". Fail Code: " + this.failCode + ". Parameter: " + this.parameter;
    }
}