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
