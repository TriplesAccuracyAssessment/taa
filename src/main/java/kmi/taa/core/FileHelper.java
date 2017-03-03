package kmi.taa.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class FileHelper {
		
	public static void writeFile(String content, String filepath, boolean append) {
		BufferedWriter writer = null;
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		try {
			writer = new BufferedWriter(new FileWriter(file, append));
			writer.write(content);

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}
	
	
	/*
	 * sorting the lines in a file according to the sorting key
	 * keyIndex - the index of the key for sorting in the file
	 */
	public static void sort(String inFile, int keyIndex, String outFile) {
		TreeMap<Integer, ArrayList<String>> tmap = new TreeMap<>();
		try {
			List<String> list = Files.readAllLines(Paths.get(inFile), StandardCharsets.UTF_8);
			String prevKey = "";
			ArrayList<String> sameKeyLines = new ArrayList<>();
			for(int i=0; i < list.size(); i++) {
				String[] str = list.get(i).split("\t");
				String currentKey = str[keyIndex];
				if(prevKey.isEmpty()) prevKey = currentKey;
				if(!tmap.containsKey(Integer.valueOf(currentKey))) {
					if(currentKey.equalsIgnoreCase(prevKey)) {
						sameKeyLines.add(list.get(i));
					} else {
						tmap.put(Integer.valueOf(prevKey), sameKeyLines);
						prevKey = currentKey;
						sameKeyLines = new ArrayList<>();
						sameKeyLines.add(list.get(i));
					}
				}
				
			}
			tmap.put(Integer.valueOf(prevKey), sameKeyLines);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile, false));
			for(Integer k:tmap.navigableKeySet()) {
				for(String line:tmap.get(k)){					
					bw.write(line);
					bw.write(System.lineSeparator());
				}
			}
			bw.close();			
		} catch (IOException e) {
			
		}
	}
	
	/*
	 * remove line ids from the input File - the line ids are at
	 * end of the lines
	 */
	public static void removeLineIds(String inFile, String outFile) throws IOException {
		StringBuilder builder = new StringBuilder();		
		List<String> list = Files.readAllLines(Paths.get(inFile), StandardCharsets.UTF_8);
		for(String line : list) {
			String[] str = line.split("\t");
			for(int i = 0; i < str.length-1; i++) {
				if(i == str.length-2)
					builder.append(str[i]);
				else
					builder.append(str[i]+"\t");				
			}
			builder.append(System.lineSeparator());
		}		
		writeFile(builder.toString(), outFile, false);
	}
	
	/*
	 * add line ids to the beginning of a file
	 */
	public static void addLineIds(String inFile, String outFile) throws IOException {
		StringBuilder builder = new StringBuilder();		
		List<String> list = Files.readAllLines(Paths.get(inFile), StandardCharsets.UTF_8);
		int id = 1;
		for(String line : list) {
			builder.append(id + "\t" + line);
			builder.append(System.lineSeparator());
			id++;
		}				
		writeFile(builder.toString(), outFile, false);
	}
	
	public static void readFileToMap(String file, HashMap<Integer, String> map) throws IOException {
		List<String> cont = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
		for(String str:cont) {
			String[] split = str.split("\t");
			map.put(Integer.valueOf(split[0]), str);
		}
	}
	
}
