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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

public class PropertyValueSelector {
	HashMap<String, String> selectedvalues = new HashMap<>();
	
	/*
	 * if the subject links and predicates of two target triples are the same, 
	 * select target triple which has the object string closest to that of the
	 * source triple (i.e. maxim string similarity)
	 */
	public void selectStringTypeSingleValue(String typecompFile, String outputFile) {
		HashMap<Integer, Vector<String>> emap = readLinesGroupbyFID(typecompFile);
		HashSet<String> delete = new HashSet<>();

		for(Integer k:emap.keySet()) {
			Vector<String> lines = emap.get(k);		
			HashSet<String> distinctpairs = new HashSet<>();
			for(int i = 0; i < lines.size(); i++) {				
				String[] line = lines.get(i).split("\t");
				if(!distinctpairs.contains(line[2]+"\t"+line[4])) {
					distinctpairs.add(line[2]+"\t"+line[4]);
				}				
			}
			
			for(String pair : distinctpairs) {
				double maxss = -1;
				String prevmax = "";
				for(int i = 0; i < lines.size(); i++) {				
					String[] line = lines.get(i).split("\t");
					if(pair.equalsIgnoreCase(line[2]+"\t"+line[4])) {
						if(maxss >= Double.valueOf(line[7])) {
							delete.add(line[8]);
						} else {							
							maxss = Double.valueOf(line[7]);
							if(i != 0) delete.add(prevmax);
							prevmax = line[8];
						}
					}
				}
			}
		}
		
		modify(typecompFile, delete, outputFile, "String");

	}
	
	public void select(String factFile, String typecompFile, String propertyType, String outputFile) {	
		OutlierDetector od = new OutlierDetector();
		od.readSourceTripleValue(factFile);
		HashMap<String, String> stmap = od.getSourceTripleValue();		
		HashMap<Integer, Vector<String>> emap = readLinesGroupbyFID(typecompFile);		
		HashSet<String> delete = new HashSet<>();
		for(Integer k:emap.keySet()) {
			Vector<String> lines = emap.get(k);			
			for(int i = 0; i < lines.size(); i++) {				
				String[] a = lines.get(i).split("\t");
				double min = 0;
				String min_id = "";
				int count = 0;				
				for(int j = i+1; j < lines.size(); j++) {
					String[] b = lines.get(j).split("\t");
					if(a[2].equalsIgnoreCase(b[2]) && a[4].equalsIgnoreCase(b[4])) {
						String tval = stmap.get(String.valueOf(k));
						if(propertyType.equalsIgnoreCase("Numerical") || propertyType.equalsIgnoreCase("Date")) {
							if(min == 0) {
								if(Math.abs(Double.valueOf(a[5]) - Double.valueOf(tval)) <= Math.abs(Double.valueOf(b[5]) - Double.valueOf(tval))) {
									min = Double.valueOf(a[5]);
									delete.add(b[7]);
									min_id = a[7];
								} else {
									min = Double.valueOf(b[5]);
									delete.add(a[7]);
									min_id = b[7];
								}
							} else {
								if(Math.abs(min - Double.valueOf(tval)) <= Math.abs(Double.valueOf(b[5]) - Double.valueOf(tval))) {
									delete.add(b[7]);
								} else {
									min = Double.valueOf(b[5]);
									delete.add(min_id);
									min_id = b[7];
								}
							} 
							count++;
						}
						
					}
				}
				
			}

		}
		
		modify(typecompFile, delete, outputFile, "numerical");
		
	}
	
	public HashMap<Integer, Vector<String>> readLinesGroupbyFID(String input) {
		HashMap<Integer, Vector<String>> map = new HashMap<>();
		try {
			List<String> lines = Files.readAllLines(Paths.get(input), StandardCharsets.UTF_8);
			for(int i = 0; i < lines.size(); i++) {
				String[] line = lines.get(i).split("\t");	
				if(!map.containsKey(Integer.valueOf(line[0]))) {
					final String ln = lines.get(i);					
					map.put(Integer.valueOf(line[0]), new Vector<String>() {{
						add(ln);
					}}
					);
				} else {
					map.get(Integer.valueOf(line[0])).add(lines.get(i));
				}
			}
		} catch (IOException e) {			

		}
		
		return map;
	}
	
	public void modify(String inputFile, HashSet<String> delete, String outputFile, String pptytype) {
		StringBuilder builder = new StringBuilder();
		try {
			List<String> lines = Files.readAllLines(Paths.get(inputFile), StandardCharsets.UTF_8);
			for(int i = 0; i < lines.size(); i++) {
				String[] line = lines.get(i).split("\t");
				if(pptytype.equalsIgnoreCase("String")) {
					if(!delete.contains(line[8])) {
						builder.append(lines.get(i));
						builder.append(System.lineSeparator());
					}
				}
				if(pptytype.equalsIgnoreCase("numerical") || pptytype.equalsIgnoreCase("date")) {
					if(!delete.contains(line[7])) {
						builder.append(lines.get(i));
						builder.append(System.lineSeparator());
					}
				}
			}
			FileHelper.writeFile(builder.toString(), outputFile, false);
		} catch (IOException e) {

		}
	}
	

}
