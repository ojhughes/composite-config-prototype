package me.ohughes.proxy;

import lombok.Getter;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.eclipse.jgit.transport.http.HttpConnection;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;
import org.eclipse.jgit.transport.http.apache.HttpClientConnection;
import org.eclipse.jgit.transport.http.apache.TemporaryBufferEntity;
import org.eclipse.jgit.transport.http.apache.internal.HttpApacheText;
import org.eclipse.jgit.util.TemporaryBuffer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jgit.util.HttpSupport.*;

/**
 * Allow JGit to be set up with configuration to enable authenticated proxies
 */

public class ProxyAwareHttpClientFactory implements HttpConnectionFactory {

	private HttpClient httpClient;
	private HttpClientConnectionManager connectionManager;
	@Getter
    private ResourceClosingHttpClientConnection httpClientConnection;

	public ProxyAwareHttpClientFactory(HttpClient httpClient) {
		this.httpClient = httpClient;
        this.connectionManager = new PoolingHttpClientConnectionManager();
    }

	@Override
	public HttpConnection create(URL url) throws IOException {
		return null;
	}

	/**
	 * JGits proxy setup mechanism doesn't work in some environments, such as containers.
	 * Override this by setting up a proxy using Apache HTTP client builder.
	 */
	@Override
	public HttpConnection create(URL url, Proxy proxy) throws IOException {
        ResourceClosingHttpClientConnection resourceClosingHttpClientConnection = new ResourceClosingHttpClientConnection(url.toString(), null, httpClient, connectionManager);
        httpClientConnection = resourceClosingHttpClientConnection;
        return resourceClosingHttpClientConnection;
	}

    /**
     * JGits client connection does not correctly consume resources, leading to memory leaks. Override the default, using
     * delegation where necessary to allow connections to be properly managed. Unfortunately there is no easy way of
     * overriding the existing {@link HttpClientConnection} class because it extensively uses field level operations, so
     * re-implementing most of the behaviour here.
     */
	public static class ResourceClosingHttpClientConnection implements HttpConnection {

	    private CloseableHttpClient client;
	    private HttpUriRequest request;
	    private CloseableHttpResponse response;
        private TemporaryBufferEntity entity;
        private URL url;
        private String name;
        private String method;
        private Integer timeout;
        private Integer readTimeout;
        private Boolean followRedirects;
        private boolean isUsingProxy;
        private SSLContext ctx;
        private HttpClientConnectionManager connectionManager;


        public ResourceClosingHttpClientConnection(String urlStr, Proxy proxy, HttpClient client, HttpClientConnectionManager connectionManager) throws MalformedURLException {
			this.client = (CloseableHttpClient) client;
            this.url = new URL(urlStr);
            this.connectionManager = connectionManager;
		}


        private void execute() throws IOException{
            if (response != null) {
                return;
            }

            if (entity == null) {
                response = client.execute(request);
                return;
            }

            try {
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntityEnclosingRequest eReq = (HttpEntityEnclosingRequest) request;
                    eReq.setEntity(entity);
                }
                response = client.execute(request);
            } finally {
                entity.close();

                entity = null;
            }
        }

        public void close() throws IOException{
            if (response != null) {
                EntityUtils.consume(response.getEntity());
                response.close();
            }
            client.close();

        }

        @Override
        public int getResponseCode() throws IOException {
            execute();
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public URL getURL() {
            return url;
        }

        @Override
        public String getResponseMessage() throws IOException {
            execute();
            return response.getStatusLine().getReasonPhrase();
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            for (Header hdr : response.getAllHeaders()) {
                List<String> list = new LinkedList<String>();
                for (HeaderElement hdrElem : hdr.getElements())
                    list.add(hdrElem.toString());
                headers.put(hdr.getName(), list);
            }
            return headers;

        }

        @Override
        public void setRequestProperty(String key, String value) {
            request.addHeader(key, value);
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
            this.method = method;
            if (METHOD_GET.equalsIgnoreCase(method)) {
                request = new HttpGet(url.toString());
            } else if (METHOD_HEAD.equalsIgnoreCase(method)) {
                request = new HttpHead(url.toString());
            } else if (METHOD_PUT.equalsIgnoreCase(method)) {
                request = new HttpPut(url.toString());
            } else if (METHOD_POST.equalsIgnoreCase(method)) {
                request = new HttpPost(url.toString());
            } else {
                this.method = null;
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void setUseCaches(boolean usecaches) {

        }

        @Override
        public void setConnectTimeout(int timeout) {
            this.timeout = Integer.valueOf(timeout);
        }

        @Override
        public void setReadTimeout(int timeout) {
            this.readTimeout = timeout;
        }

        @Override
        public String getContentType() {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                Header contentType = responseEntity.getContentType();
                if (contentType != null)
                    return contentType.getValue();
            }
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return response.getEntity().getContent();
        }

        @Override
        public String getHeaderField(String name) {
            Header header = response.getFirstHeader(name);
            return (header == null) ? null : header.getValue();
        }

        @Override
        public int getContentLength() {
            Header contentLength = response.getFirstHeader("content-length"); //$NON-NLS-1$
            if (contentLength == null) {
                return -1;
            }

            try {
                int l = Integer.parseInt(contentLength.getValue());
                return l < 0 ? -1 : l;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        @Override
        public void setInstanceFollowRedirects(boolean followRedirects) {
            this.followRedirects = Boolean.valueOf(followRedirects);
        }

        @Override
        public void setDoOutput(boolean dooutput) {

        }

        @Override
        public void setFixedLengthStreamingMode(int contentLength) {

        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (entity == null)
                entity = new TemporaryBufferEntity(new TemporaryBuffer.LocalFile(null));
            return entity.getBuffer();
        }


        @Override
        public void setChunkedStreamingMode(int chunklen) {
            if (entity == null)
                entity = new TemporaryBufferEntity(new TemporaryBuffer.LocalFile(null));
            entity.setChunked(true);
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public boolean usingProxy() {
            return isUsingProxy;
        }

        @Override
        public void connect() throws IOException {
            execute();
        }

        @Override
        public void configure(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws NoSuchAlgorithmException, KeyManagementException {
            getSSLContext().init(km, tm, random);
        }

        @Override
        public void setHostnameVerifier(HostnameVerifier hostnameverifier) throws NoSuchAlgorithmException, KeyManagementException {

        }
        private SSLContext getSSLContext() {
            if (ctx == null) {
                try {
                    ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(
                            HttpApacheText.get().unexpectedSSLContextException, e);
                }
            }
            return ctx;
        }


    }
}
