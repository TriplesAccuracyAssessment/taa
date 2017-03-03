package kmi.taa.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class SPARQLHTTPClient {

	
	public String httpGet(String url, String proxy) throws ClientProtocolException, IOException {		
		CloseableHttpClient httpclient = HttpClients.createDefault();		
		HttpGet httpget = new HttpGet(url);
		String responseBody;
		try {			
			if(!proxy.isEmpty()) { 
				String[] str = proxy.split(":");
				int port = Integer.parseInt(str[1]);
				HttpHost host = new HttpHost(str[0], port, str[2]);
	            RequestConfig config = RequestConfig.custom()
	                    .setProxy(host)
	                    .build();         	            
	            httpget.setConfig(config);
			}
			
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						try{
							HttpEntity entity = response.getEntity();
							return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8): null;
						} catch (ClientProtocolException e) {							
							return "";
						}
					}
					return "";
				}
				
			};
			
			responseBody = httpclient.execute(httpget, responseHandler);
		} finally {
			httpclient.close();	
		}
		
		return responseBody;
	}
	

}
