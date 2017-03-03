package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class OutlierDetector {
	
	protected HashMap<String, String> stValues = new HashMap<>();
	protected HashMap<Integer, String> similarityIns = new HashMap<>(); 
	
	protected InterquartileRange IR = new InterquartileRange();
	
	public HashMap<String, String> getSourceTripleValue() {
		return stValues;
	}

	// run the outlier detector
	public void start(String pptytype, String similarityFile, int columnIndex, String triplesFile, String outliersFile) {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		
		readSourceTripleValue(triplesFile);
		readSimilarityInstances(similarityFile);
		
		System.out.println(System.currentTimeMillis()+": IRange detecting...");

		try {
			br = new BufferedReader(new FileReader(similarityFile));
			String id = "";
			HashMap<Integer, Double> values = new HashMap<>();			
			while((line = br.readLine()) != null) {							
				String[] instance = line.split("\t");
				if(!stValues.containsKey(instance[0])) {
					continue;
				} else {
					if(pptytype.equalsIgnoreCase("Date")) {	
						double year = getYear(stValues.get(instance[0]));	
						if(Double.valueOf(instance[5]) == year) {
							builder.append(line + "\tno");
							builder.append(System.lineSeparator());
							continue;
						}
					}								
					if(id.equals("")) {
						id = instance[0];
					}				
					if(id.equals(instance[0])) {
						values.put(Integer.valueOf(instance[7]), Double.valueOf(instance[columnIndex]));
					}
					else {																		
						values.put(Integer.valueOf(0), Double.valueOf(stValues.get(id)));						
						double[] val = new double[values.size()];
						int i = 0;
						for(Integer k:values.keySet()) {
							val[i++] = values.get(k).doubleValue();
						}						
						Arrays.sort(val);					
						IR.calculateQuartiles(val);			
						for(Integer k: values.keySet()) {
							if(k.intValue() != 0) {								
								boolean result = IR.isOutlier(values.get(k).doubleValue());
								if(result) {									
									builder.append(similarityIns.get(k) + "\tyes");
								}
								else {
									builder.append(similarityIns.get(k) + "\tno");
								}
								builder.append(System.lineSeparator());							
							}
						}						
						values.clear();
						values.put(Integer.valueOf(instance[7]), Double.valueOf(instance[columnIndex]));
						id = instance[0];		
					}
				}

			}

			values.put(Integer.valueOf(0), Double.valueOf(stValues.get(id)));			
			double[] val = new double[values.size()];
			int i = 0;
			for(Integer k:values.keySet()) {
				val[i++] = values.get(k).doubleValue();
			}
			
			Arrays.sort(val);
			IR.calculateQuartiles(val);
			
			for(Integer k: values.keySet()) {				
				if(k.intValue() != 0) {
					boolean result = IR.isOutlier(values.get(k).doubleValue());					
					if(result) {
						builder.append(similarityIns.get(k) + "\tyes");
					}
					else {
						builder.append(similarityIns.get(k) + "\tno");
					}
					builder.append(System.lineSeparator());							
				}
			}
			
			FileHelper.writeFile(builder.toString(), outliersFile, false);
			
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			
		} catch (NullPointerException e) {
			
		}
		
		// sort the unordered outputFile
		FileHelper.sort(outliersFile, 7, outliersFile);
		
		System.out.println(System.currentTimeMillis()+": completed");
	}
	
	protected void readSourceTripleValue(String targetTriplesFile) {
		BufferedReader br = null;
		String line = "";
		
		try {
			br = new BufferedReader (new FileReader(targetTriplesFile));
			while((line = br.readLine()) != null) {
				String[] instance = line.split("\t");
				stValues.put(instance[0], instance[3]);
			}
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}
	}
	
	protected void readSimilarityInstances(String similarityFile) {
		BufferedReader br = null;
		String line = "";
		
		try {
			br = new BufferedReader (new FileReader(similarityFile));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				similarityIns.put(Integer.valueOf(str[7]), line);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public double getYear(String date) {	
		double d = Double.valueOf(date) * 1000; // convert to milliseconds
		Date dat = new Date((long) d);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		PropertyTypeComparison ptc = new PropertyTypeComparison();
		return ptc.DateToSeconds(sdf.format(dat));
	}

}
