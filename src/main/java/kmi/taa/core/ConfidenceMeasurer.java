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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ConfidenceMeasurer {
	final static Logger log = Logger.getLogger(ConfidenceMeasurer.class.getName());
	private HashMap<Integer, String[]> factTable = new HashMap<>();
	private ArrayList<String[]> evigroup;
	StringBuilder builder = new StringBuilder();
	StringBuilder unmatched = new StringBuilder(); 
	StringBuilder facbuilder = new StringBuilder();
	
	private void readFacts(String factFile) throws IOException {
		BufferedReader br = null;
		String line = "";
		String linesplit = "\t";
		
		try {
			br = new BufferedReader(new FileReader(factFile));
			while((line = br.readLine()) != null) {
				String[] r = line.split(linesplit);
				String[] fact = new String[4];
				fact[0] = r[0];
				fact[1] = r[1];
				fact[2] = r[2];
				fact[3] = r[3];
				factTable.put(Integer.valueOf(r[0]), fact);				
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		
	}
	
	/*
	 * fact file: a row represents a fact (fid, fact_s, fact_p, fact_o)
	 * evidence file: consists of several columns (fid, fact_s, evidence_s, type of sameAs service, evidence_p, evidence_o, sw, ps, os (in case it is string))
	 * output file: fid, fact_s, fact_p, fact_o, confidence
	 *   
	 */
	public void run(String factFile, String evidenceFile, String outputFile, String file, String NDfile, boolean isNumericalType) throws IOException {
		BufferedReader br = null;
		String line = "";
		readFacts(factFile);				
		try {
			br = new BufferedReader(new FileReader(evidenceFile));  
			String prevId = "";				
			System.out.println(System.currentTimeMillis()+": starting confidence score calculation...");
			while((line = br.readLine()) != null) {				
				String[] cols = line.split("\t");
				//if the object value is a numerical type				
				if(isNumericalType) {
					// first evidence triple
					if(prevId.equals("")) {
						evigroup = new ArrayList<String[]>();
						evigroup.add(cols);
						prevId = cols[0];
						continue;
					}										
					if(!prevId.equals(cols[0])) {// starts the process for a new target fact	
						calNumericalTypeConfidence(evigroup, prevId);
						// remove fact when it is processed - to get a list of unprocessed facts
						factTable.remove(Integer.valueOf(prevId)); 
						evigroup = new ArrayList<String[]>();
						evigroup.add(cols);						
					} else { // continue if it is not for a new target fact						
						evigroup.add(cols);
					}					
					prevId = cols[0];
					
				} else {// if the object value is type of string	
					if(prevId.equals("")) {
						evigroup = new ArrayList<String[]>();
						evigroup.add(cols);
						prevId = cols[0];
						continue;
					}						
					if(!prevId.equals(cols[0])) {
						calStringTypeConfidence(evigroup, prevId);
						factTable.remove(Integer.valueOf(prevId));
						evigroup = new ArrayList<String[]>();
						evigroup.add(cols);															
					} else { //continue if it is not a new fact					
						evigroup.add(cols);
					}
					prevId = cols[0];
				}

			}
			
			if(!evigroup.isEmpty()) {// calculate score when only one evidence triple or come to the end of the evidence file
				if (isNumericalType) {
					calNumericalTypeConfidence(evigroup, prevId);
					factTable.remove(Integer.valueOf(prevId));
				} else {
					calStringTypeConfidence(evigroup, prevId);					
				}
			}
			
		} catch (NullPointerException e) {
			log.error("The matched triples file is empty!", e);
			return;
		}
		finally {
			if(br != null) {
				br.close();
			}
		}
		
		for(Integer fid: factTable.keySet()) {			
			facbuilder.append(factTable.get(fid)[0]+"\t"+factTable.get(fid)[1]+"\t"+factTable.get(fid)[2]+"\t"+"NF"); // NF: matched triples not found
			facbuilder.append(System.lineSeparator());
		}
		
		FileHelper.writeFile(builder.toString(), outputFile, false);
		builder.setLength(0);
		FileHelper.writeFile(unmatched.toString(), file, false);
		unmatched.setLength(0);
		FileHelper.writeFile(facbuilder.toString(), NDfile, false);
		facbuilder.setLength(0);
		System.out.println(System.currentTimeMillis()+": completed");
	}
	

	private void calNumericalTypeConfidence(ArrayList<String[]> array, String fid) {
		double weighted_sum_obj = 0;
		double sum_weights = 0;
		
		for(String[] a : array) {
			try{
				weighted_sum_obj += Double.parseDouble(a[6]) * Integer.parseInt(a[7]) * Double.parseDouble(a[5]);
				sum_weights += Double.parseDouble(a[6]) * Integer.parseInt(a[7]);
			} catch(NumberFormatException e) {
				unmatched.append(a[0]+"\t"+a[1]+"\t"+a[2]+"\t"+a[4]+"\t"+"object type not matched");
				unmatched.append(System.lineSeparator());				
			}
		}
		

		double avg = weighted_sum_obj / sum_weights;		
		String[] fact = factTable.get(Integer.valueOf(fid));
		double confidence = 0;
		try{						
			// calculate confidence score
			if(Double.valueOf(fact[3]) == avg && avg == 0) {
				confidence = 1;
			} else {
			confidence = 1 - Math.abs(Double.valueOf(fact[3]) - avg) / Math.max(Math.abs(avg), Math.abs(Double.valueOf(fact[3])));			
			}
			builder.append(fact[0]+"\t"+fact[1]+"\t"+fact[2]+"\t"+fact[3]+"\t"+confidence);
			builder.append(System.lineSeparator());
			
		} catch(NumberFormatException e) {
			log.error("source triple value error", e);  
		}
				
	}
	
	private void calStringTypeConfidence(ArrayList<String[]> array, String fid) {
		double weighted_sum_obj = 0;
		double sum_weights = 0;
		
		for(String[] a : array) {
			try{
				weighted_sum_obj += Double.parseDouble(a[6]) * Integer.parseInt(a[8]) * Double.parseDouble(a[7]);
				sum_weights += Double.parseDouble(a[6]) * Integer.parseInt(a[8]);
			} catch(NumberFormatException e) {
				log.error("calculate string type confidence error", e);
			}
		}
		double confidence = weighted_sum_obj / sum_weights;	
		String[] fact = factTable.get(Integer.valueOf(fid));
		builder.append(fact[0]+"\t"+fact[1]+"\t"+fact[2]+"\t"+fact[3]+"\t"+confidence);
		builder.append(System.lineSeparator());
	}
	
}
