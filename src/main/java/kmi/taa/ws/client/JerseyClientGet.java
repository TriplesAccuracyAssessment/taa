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
package kmi.taa.ws.client;

import java.io.StringReader;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.glassfish.jersey.client.ClientConfig;

import kmi.taa.ws.jaxb.stands4.Results;

public class JerseyClientGet {	

	public String get(String uri) {
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		WebTarget target = client.target(uri);
		String xmlRes = target.request().get(String.class);
		return xmlRes;

	}

	public String unMarshallStands4(String uri) {
		String xmlRes = get(uri);
		Results rs = null;
		String de = "";
		try {
			JAXBContext context = JAXBContext.newInstance(Results.class);
			Unmarshaller um = context.createUnmarshaller();
			rs = (Results) um.unmarshal(new StringReader(xmlRes));
			
		} catch (JAXBException e) {
			e.printStackTrace();
		} 
		
		try {
			de = rs.getResults().get(0).getDefinition();
		} catch(NullPointerException e) {

		}
		
		return de;  
	}
}
