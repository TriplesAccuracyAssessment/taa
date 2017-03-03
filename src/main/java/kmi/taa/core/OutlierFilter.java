package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class OutlierFilter {
	
	/*
	 * remove lines marked as outliers
	 */
	public void run(String outlierFile, String outputFile) {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
				
		try {
			br = new BufferedReader(new FileReader(outlierFile));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");			
				if(str[8].trim().equalsIgnoreCase("no")) {
					builder.append(line);
					builder.append(System.lineSeparator());
				}					
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		FileHelper.writeFile(builder.toString(), outputFile, false);
	}
	
	/*
	 *  clean up the marking labels
	 */
	public void clean(String outlierFile, String outputFile) {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
				
		try {
			br = new BufferedReader(new FileReader(outlierFile));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");			
				if(str[8].trim().equalsIgnoreCase("no")) {
					builder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+str[3]+"\t"+str[4]+"\t"+str[5]+"\t"+str[6]);
					builder.append(System.lineSeparator());
				}					
			}
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			
		}		
		FileHelper.writeFile(builder.toString(), outputFile, false);
	}
}
