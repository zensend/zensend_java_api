package io.zensend;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

public class FixedKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    private final long timeoutInMilliseconds;
    
    public FixedKeepAliveStrategy(long timeoutInMilliseconds) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        return this.timeoutInMilliseconds;
    }

    
}
