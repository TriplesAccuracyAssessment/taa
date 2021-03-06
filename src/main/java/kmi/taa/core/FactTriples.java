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
