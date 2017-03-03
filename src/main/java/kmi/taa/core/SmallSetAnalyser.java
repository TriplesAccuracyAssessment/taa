package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class SmallSetAnalyser {
	
	protected HashMap<Integer, HashMap<Integer, Double>> hm = new HashMap<>();
	protected HashMap<Integer, Double> hmf = new HashMap<>();
	protected Vector<Integer> notol = new Vector<>();
		
	public void run(String factTriplesFile, String outlierrmFile, String outputFile, double threshold, String pptytype) {		
		hm.clear();
		hmf.clear();
		notol.clear();		
		readSourceTriples(factTriplesFile);			
		constructSmallSet(outlierrmFile);
		//detect outliers from a small set of numbers
		detect(threshold, pptytype);				
		//create the output file
		write(outlierrmFile, outputFile);
		
	}
	
	protected void readSourceTriples(String sourceTriplesFile) {
		 BufferedReader br = null;
		 String line = "";		 
		 try {
			br = new BufferedReader(new FileReader(sourceTriplesFile));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");				
				if(!hmf.containsKey(Integer.valueOf(str[0]))) {
					hmf.put(Integer.valueOf(str[0]), Double.valueOf(str[3]));
				}
			}
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void constructSmallSet(String outlierrmFile) {
		BufferedReader br = null;
		String line = "";
		try {
			br = new BufferedReader(new FileReader(outlierrmFile));
			while((line = br.readLine()) != null) {
				final String[] str = line.split("\t");
				if(!hm.containsKey(Integer.valueOf(str[0]))) {
					hm.put(Integer.valueOf(str[0]), new HashMap<Integer, Double>() {{
						put(Integer.valueOf(str[7]), Double.valueOf(str[5]));
					}});
				} else {
					hm.get(Integer.valueOf(str[0])).put(Integer.valueOf(str[7]), Double.valueOf(str[5]));
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		for(Integer i:hm.keySet()) {
			hm.get(i).put(0, hmf.get(i)); 
		}

	}

	protected void detect(double threshold, String pptytype) {		
		if(hm.isEmpty()) {
			return;
		}		
		
		for(Integer k: hm.keySet()) {
			HashMap<Integer, Double> totalset =	hm.get(k);						
			if(totalset.size() > 2 && totalset.size() <= 4) {
				double mean = mean(totalset);
				double std = std(totalset);
				for(Integer tid:totalset.keySet()) {
					if(tid == 0) continue;
					double data = totalset.get(tid);
					if(Math.abs(data - mean) <= threshold * std) {
						notol.add(tid);
					}
				}
			} else {
				for(Integer tid:totalset.keySet()) {
					if(tid == 0) continue;
					notol.add(tid);
				}
			}	
		}		
								
	}
	
	protected void write(String outlierrmFile, String outputFile) {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(outlierrmFile));
			while((line = br.readLine()) != null) {
				String[] str = line.split("\t");
				if(notol.contains(Integer.valueOf(str[7]))) {					builder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+str[3]+"\t"+str[4]+"\t"+str[5]+"\t"+str[6]+"\t"+str[7]+"\t"+"no");
				}else {					builder.append(str[0]+"\t"+str[1]+"\t"+str[2]+"\t"+str[3]+"\t"+str[4]+"\t"+str[5]+"\t"+str[6]+"\t"+str[7]+"\t"+"yes");
				}
				builder.append(System.lineSeparator());				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileHelper.writeFile(builder.toString(), outputFile, false);
		
	}
	
	public double mean(HashMap<Integer, Double> map) {
		double sum = 0;
		for(Double v:map.values()) {
			sum += v.doubleValue();
		}
		return sum/map.size();
	}
	
	public double std(HashMap<Integer, Double> map) {
		double[] va = new double[map.size()];
		int i=0;
		for(Integer k:map.keySet()) {
			va[i++] = map.get(k).doubleValue();
		}
		DescriptiveStatistics ds = new DescriptiveStatistics(va);
		return ds.getStandardDeviation();
	}
	
	public void addLineids(Path path, String output) {
		StringBuilder builder = new StringBuilder();
		
		try {
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			int i=1;
			for(String l:lines) {
				builder.append(l+"\t"+i++);
				builder.append(System.lineSeparator());
			}
		} catch (IOException e) {
			
		}
		
		FileHelper.writeFile(builder.toString(), output, false);
	}
	

}
