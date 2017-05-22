package me.ohughes.proxy.properties;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password")
public class ProxyHostProperties {

	private String host;
	private int port;
	private String nonProxyHosts;
	private String username;
	private String password;
}
