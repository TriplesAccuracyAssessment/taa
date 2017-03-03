/*
 * (C) Copyright 2017 Shuangyan Liu
 * Shuangyan.Liu@open.ac.uk 
 * Knowledge Media Institute
 * The Open University, United Kingdom
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
