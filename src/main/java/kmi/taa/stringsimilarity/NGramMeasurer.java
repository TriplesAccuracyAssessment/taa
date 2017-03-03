package kmi.taa.stringsimilarity;

import databionics.text.SimilarString;

public class NGramMeasurer {
	
	public static double measure(String str1, String str2) {
		SimilarString cmp = new SimilarString(str1);
		float score = cmp.nGramMetric(3, str2);	
		return score;
	}

}
