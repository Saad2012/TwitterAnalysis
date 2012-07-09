package com.twitter.corpus.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class VolatilityAnalysis {
	
	public static void avgVolatility(HashMap<Integer, HashMap<Integer,Double>> jaccard, String path) throws IOException{
	
		HashMap<Integer, Double> avgVolMap = new HashMap<Integer, Double>(jaccard.size());
		HashMap<Integer, Double> stdDevMap = new HashMap<Integer, Double>(jaccard.size());
		// iterate terms
		Iterator<Entry<Integer, HashMap<Integer, Double>>> iter = jaccard.entrySet().iterator();
		while(iter.hasNext()){
			Entry<Integer, HashMap<Integer, Double>> entry = iter.next();
			Integer term = entry.getKey();
			
			// iterate termsets
			Iterator<Map.Entry<Integer, Double>> jaccEntry = entry.getValue().entrySet().iterator();
			Double avgVol = 0.0;
			
			// calc mean
			
			while (jaccEntry.hasNext()) {
				avgVol += jaccEntry.next().getValue();
			}
			avgVol /= 32;
			avgVol = (double)Math.round(avgVol * 1000) / 1000;
			avgVolMap.put(term, avgVol);
			
			// using mean calc std deviation			
			
			Iterator<Map.Entry<Integer, Double>> jaccEntry2 = entry.getValue().entrySet().iterator();

			Double num = 0.0;
			while (jaccEntry2.hasNext()) {
				// sqrt of the absolute value
				num += Math.sqrt(Math.abs(jaccEntry2.next().getValue() - avgVol));				
			}
			Double stdDev = Math.sqrt(num / 32);
			stdDev = (double)Math.round(stdDev * 1000) / 1000;
			
			stdDevMap.put(term, stdDev);
		}
		printAnalysisResults(avgVolMap, path, "avgVol2.csv");
		printAnalysisResults(stdDevMap, path, "stdDev2.csv");
	}
	public static void printAnalysisResults(HashMap<Integer, Double> avgVol, String path, String name) throws IOException{
		
		BufferedWriter bf =  new BufferedWriter(new FileWriter(path + name));
			
		Iterator<Map.Entry<Integer, Double>> volIter = avgVol.entrySet().iterator();
		while(volIter.hasNext()){
			Entry<Integer, Double> entry = volIter.next();
			bf.append(entry.getKey() + "\t" + entry.getValue() + "\n");
		}
	}
}
