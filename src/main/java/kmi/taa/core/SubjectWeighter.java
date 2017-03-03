package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SubjectWeighter {
	
	public void run(String inputFile, String outputFile) throws IOException {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(inputFile));  
			System.out.println(System.currentTimeMillis()+": assigning reliability of subjects...");
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				if(str[3].equals("owl:sameAs")) {
					builder.append(line+"\t4");
					builder.append(System.lineSeparator());
				}
				if(str[3].equals("sameas.org service")) {
					builder.append(line+"\t3");
					builder.append(System.lineSeparator());
				}
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		
		FileHelper.writeFile(builder.toString(), outputFile, false);
		System.out.println(System.currentTimeMillis()+": completed");
	}

}
