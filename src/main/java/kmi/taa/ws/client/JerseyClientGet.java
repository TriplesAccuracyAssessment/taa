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
