package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FactTriples {
	
	public void convertDate(String filename, String output) {				
		BufferedReader br = null;
		StringBuilder builder = new StringBuilder();
		String line;
		PropertyTypeComparison ptcom = new PropertyTypeComparison();
		
		try {
			br = new BufferedReader(new FileReader(filename));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				builder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+ptcom.DateToSeconds(str[3]));
				builder.append(System.lineSeparator());				
			}
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			
		}
		
		FileHelper.writeFile(builder.toString(), output, false);
		
	}
}
