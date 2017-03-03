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
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PropertyTypeComparison {
	
	public void run(String pptytype, String inputFile, String outputFile) {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		StringBuilder outputBuilder = new StringBuilder();
		
		System.out.println(System.currentTimeMillis()+": start property type comparison: ");		
			try {
				br = new BufferedReader(new FileReader(inputFile));
				while((line = br.readLine()) != null) {					
					String[] str = line.split("\t");					
						try {
							String[] obj = str[5].split(",");
							if(pptytype.equalsIgnoreCase("Numerical")) {
								Double value = Double.valueOf(obj[0]);
								outputBuilder.append(line);
								outputBuilder.append(System.lineSeparator());
							}
							if(pptytype.equalsIgnoreCase("Date")) {
								if(isDate(obj[0])) {									outputBuilder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+str[3]+"\t"+str[4]+"\t"+DateToSeconds(obj[0])
											+"\t"+str[6]);
									outputBuilder.append(System.lineSeparator());		
								} else {
									builder.append(line + "\t not_date_type");
									builder.append(System.lineSeparator());
								}
							}	

							if(pptytype.equalsIgnoreCase("String")) {
								if(!isNumerical(obj[0]) && !isDate(obj[0])) {
									outputBuilder.append(line);
									outputBuilder.append(System.lineSeparator());
								} else {
									builder.append(line);
									builder.append(System.lineSeparator());
								}
							}
							
						} catch(NumberFormatException e) {
							builder.append(line+"\t not_numerical_type");
							builder.append(System.lineSeparator());
						} 

					}					
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
							
		
		FileHelper.writeFile(outputBuilder.toString(), outputFile, false);
		FileHelper.writeFile(builder.toString(), outputFile.replace("typecomp", "typecomp_notmatched"), false);
		System.out.println(System.currentTimeMillis()+": property type comparison is completed");
	}

	public boolean isNumerical(String str) {
		boolean numerical = false;
		try {
			Double value = Double.valueOf(str);
			numerical = true;
		} catch (NumberFormatException e) {
			
		}
		
		return numerical;
	}
	
	public boolean isDate(String input) {
		boolean result = false;
		SimpleDateFormat sdf = new SimpleDateFormat();
		String[] patterns = {"yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM"};
		Date date;
		
		int year = Calendar.getInstance().get(Calendar.YEAR);
		if(input.length() <= 4) {			
			sdf.applyPattern("yyyy");
			try {
				date = sdf.parse(input);
				if(date.getYear() < year) {
					result = true;
				} else {
					result = false;
				}
			} catch (ParseException e) {
			}
		}
		
		for(String pat:patterns) {
			sdf.applyPattern(pat);
			try {
				date = sdf.parse(input);
			} catch (ParseException e) {
				continue;
			}
			result = true;
		}
		
		return result;
	}

	
	/*
	 *  convert Date (in String format) to seconds with 2 decimal places and rounded up
	 *  to the 2nd decimal digit.
	 *  from JDK 1.1, Date(String s) is deprecated.
	 */	
	public double DateToSeconds(String input) {
		double result = 0;		
		SimpleDateFormat sdf = new SimpleDateFormat();
		String[] patterns = {"yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM"};
		Date date;
		
		if(input.length() <= 4) {		
			sdf.applyPattern("yyyy");
			try {
				date = sdf.parse(input);				
				result = Math.round(date.getTime() / (double) 1000 * 100.0) / 100.0;
			} catch (ParseException e) {
			}
		} else {

		for(String pat:patterns) {	
			String rep = pat.replaceAll("'", "");
			if(input.length() == rep.length()) {
				sdf.applyPattern(pat);
				try {
					date = sdf.parse(input);
					result = Math.round(date.getTime() / (double) 1000 * 100.0) / 100.0;
				} catch (ParseException e) {
					continue;
				}
			}
		}
		}

		return result;
	}

}

