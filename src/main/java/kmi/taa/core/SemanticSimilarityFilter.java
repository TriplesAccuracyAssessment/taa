package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SemanticSimilarityFilter {
	
	public void filter(double threshold, String in, String ou) {
		 BufferedReader br = null;
		 String line = "";
		 StringBuilder builder = new StringBuilder();		 		 
		 
		 try {
			br = new BufferedReader(new FileReader(in));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				if(Double.parseDouble(str[6]) >= threshold) {
					builder.append(line);														
					builder.append(System.lineSeparator());
				}
			}
		} catch (FileNotFoundException e) {			
		} catch (IOException e) {			
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		 
		 FileHelper.writeFile(builder.toString(), ou, false);		
		 
	}
}
