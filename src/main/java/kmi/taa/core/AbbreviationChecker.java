package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import kmi.taa.ws.client.JerseyClientGet;

public class AbbreviationChecker {
	
	public void check(String ppty, String in, String ou) {
		 BufferedReader br = null;
		 String line = "";
		 StringBuilder builder = new StringBuilder();
		 
		 String abbr = getAbbrFromStands4API(ppty);
		 
		 try {
			br = new BufferedReader(new FileReader(in));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				if(str[4].equalsIgnoreCase(abbr)) {
					builder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+str[3]+"\t"+str[4]+"\t"+str[5]+"\t"+"1.0");										
				} else {
					builder.append(line);
				}
				builder.append(System.lineSeparator());
			}
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		 
		FileHelper.writeFile(builder.toString(), ou, false);		
		 
	}
	
	private String getAbbrFromStands4API(String ppty) {
		JerseyClientGet jcg = new JerseyClientGet();
		String abbr = "";
		int i = 2;		
		while(i < ppty.length()) {
			String tmp = ppty.substring(0, i+1);
			String definition = jcg.unMarshallStands4("http://www.stands4.com/services/v2/abbr.php?uid=4183&tokenid=3YXfozbJ0lO9aTUp&term="+tmp+"&sortby=p");
			if(ppty.equalsIgnoreCase(definition)) {
				abbr = tmp;
				break;
			}
			i++;
		}
		return abbr;
	}
}
