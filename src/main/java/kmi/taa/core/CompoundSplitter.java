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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class CompoundSplitter {
	final static Logger log = Logger.getLogger(CompoundSplitter.class.getName());
	HashMap<String, String> wikiprop = new HashMap<>();
	
	public CompoundSplitter(String filepath) {
		loadWikiProperties(filepath);
	}
	
	public void extractFormatLabels(String inputFile, String outputFile) throws IOException {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(inputFile));
			System.out.println(System.currentTimeMillis()+": splitting compounds...");
			while((line = br.readLine()) != null) {
				String[] r = line.split("\t");
				if(r.length >= 6) {
					if(r[4].contains("#")) {					
						String[] str = r[4].split("#");					
						builder.append(r[0]+"\t"+r[1]+"\t"+r[2]+"\t"+r[3]+"\t"+formatLabel(str[1])+"\t"+r[5]);
						builder.append(System.lineSeparator());
					} else if(r[4].contains("http://www.wikidata.org/prop/direct/")) {
						String[] str = r[4].split("/");
						String pptyname = wikiprop.get(str[str.length-1]);
						builder.append(r[0]+"\t"+r[1]+"\t"+r[2]+"\t"+r[3]+"\t"+pptyname+"\t"+r[5]);
						builder.append(System.lineSeparator());
					} else {
						String[] str = r[4].split("/");
						builder.append(r[0]+"\t"+r[1]+"\t"+r[2]+"\t"+r[3]+"\t"+formatLabel(str[str.length-1])+"\t"+r[5]);
						builder.append(System.lineSeparator());
					}
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
	
	//Split compound words that considers common cases of label's features
	public String formatLabel(String label) {
		StringBuilder builder = new StringBuilder();
		
		if(label.contains(" ")) return label;
		if(label.contains("_")) {
			String formatted = label.replace("_", " ");
			formatted = formatted.replace(":", " ");
			return formatted;
		}

		char[] A = label.toCharArray();
		int i = 0;
		while(i < A.length) {
			if('A'<= A[i] && A[i] <= 'Z') {
				builder.append(" "+A[i]);	
				i++;
				while(i<A.length) {					
					if(('A'<= A[i] && A[i] <= 'Z') || ('0' <= A[i] && A[i] <= '9')) {
						builder.append(A[i]);
						i++;
					}
					else break;
				}
			}
			else {
				builder.append(A[i]);
				i++;
			}
		}
		
		return builder.toString();
	}
	
	// get wikidata properties label from resource file
	public void loadWikiProperties(String file) {
		BufferedReader br = null;
		String line = "";
		
		try {
			br = new BufferedReader(new FileReader(file));
			while((line = br.readLine()) != null) {
				String[] str = line.split(",");
				if(str[0].equalsIgnoreCase("property")) {
					continue;
				}else {
					String[] uri = str[0].split("/");
					wikiprop.put(uri[uri.length-1], str[1]);
				}
			}
		} catch (FileNotFoundException e) {
			log.error("wiki properties file not found", e);
		} catch (IOException e) {
			log.error("load wiki properties file error", e);
		}
		
	}
		
	public void run(String dirOutput) throws IOException {
		extractFormatLabels(dirOutput+"predicates_objects_sorted.txt", dirOutput+"formatted_predicates.txt");
	}
	

}
