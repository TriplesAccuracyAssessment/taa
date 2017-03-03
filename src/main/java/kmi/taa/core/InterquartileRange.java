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

public class InterquartileRange {
	
	double of = 1.5; // default outlier factor
	double UpperOutlier;
	double LowerOutlier;
	
	public void calculateQuartiles(double[] sortedArray) {
		double q1;
		double q3;
		
		int half = sortedArray.length / 2;
		int quarter = half / 2;
		
		if(half % 2 == 1) {//if size of the data is an odd
			q1 = sortedArray[quarter];
			q3 = sortedArray[sortedArray.length - 1 - quarter];
		}
		else {//if size of the data is an even
			q1 = (sortedArray[quarter] + sortedArray[quarter+1]) /2;
			q3 = (sortedArray[sortedArray.length - 1 - quarter] + sortedArray[sortedArray.length - quarter]) /2;
		}
		
		double IQR = q3 - q1;
		UpperOutlier = q3 + getOutlierFactor() * IQR;
		LowerOutlier = q1 - getOutlierFactor() * IQR;
	}
	
	public double getOutlierFactor() {
		return of;
	}
	
	public void setOutlierFactor(double factor) {
		of = factor;
	}
	
	public boolean isOutlier(double value) {
		boolean result;
		result = (UpperOutlier < value) || (value < LowerOutlier);
		return result;
	}

}
