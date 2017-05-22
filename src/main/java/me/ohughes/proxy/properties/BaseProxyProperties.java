package me.ohughes.proxy.properties;

import lombok.Data;

@Data
public class BaseProxyProperties {
    private HttpsProxyProperties https;
    private HttpProxyProperties http;
}
