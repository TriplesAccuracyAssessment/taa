package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class SemanticSimilarityCalculator {	
	private String pyPath;
	private String mPath;
	
	public SemanticSimilarityCalculator(String pyPath, String mPath) {
		this.pyPath = pyPath;
		this.mPath = mPath;
	}
	
	public void run(HashMap<String, Double> map, String predicate, String inputFile, String outputFile) throws IOException {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder1 = new StringBuilder();		
		try {
			br = new BufferedReader(new FileReader(inputFile));
			System.out.println(System.currentTimeMillis()+": calculating semantic similarity for the predicate "+predicate+"...");
			while((line = br.readLine()) != null) {				
				String[] r = line.split("\t");
				double score = 0;
				try {
					if(!map.containsKey(predicate+"_"+r[4])) {
						score = runSentencesComparision(predicate, r[4]);
						map.put(predicate+"_"+r[4], score);
					} else {
						score = map.get(predicate+"_"+r[4]);
					}
					builder1.append(line+"\t"+score);
					builder1.append(System.lineSeparator());
				} catch (ArrayIndexOutOfBoundsException e) {
				}

			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
				
		FileHelper.writeFile(builder1.toString(), outputFile, false);		
		System.out.println(System.currentTimeMillis()+": completed");
	}

	public double runSentencesComparision(String sentence1, String sentence2) throws IOException {
		String[] words1 = sentence1.split(" ");
		String[] words2 = sentence2.split(" ");		
		return icPredicateSim(words1, words2);
	}
	
	//calculate information content based predicate similarity 
	public double icPredicateSim(String[] s1, String[] s2) throws IOException {
		StringBuilder builder = new StringBuilder();
		for(int i = 0 ; i < s1.length; i++) {
			if(i == s1.length-1)
				builder.append(s1[i]);
			else
				builder.append(s1[i] + "_");
		}
		String arg1 = builder.toString();
		
		builder.setLength(0);
		for(int i = 0 ; i < s2.length; i++) {
			if(i == s2.length-1)
				builder.append(s2[i]);
			else
				builder.append(s2[i] + "_");
		}
		String arg2 = builder.toString();
		
		System.out.println(arg1);
		System.out.println(arg2);
		String[] cmd = {
			      pyPath,
			      mPath+"similarity.py",
			      arg1,
			      arg2
		};		
		Process p = Runtime.getRuntime().exec(cmd);
		BufferedReader stdInput = new BufferedReader(new
              InputStreamReader(p.getInputStream()));	
		String s;
		return (s = stdInput.readLine()) != null? Double.valueOf(s) : 0;
		
	}
	
}