package com.twitter.corpus.translation;

import java.io.IOException;
import java.util.HashMap;

import com.twitter.corpus.types.SerializationHelper;


/**
 * 
 * Deserialise Jaccard data for avg volatility calculations
 *
 */
public class JaccardDeserializer {

	public static void main(String[] args) throws IOException{
		String path = "/home/dock/Documents/IR/AmazonResults/mRange3/tc_0.15/";
		String file = "jaccardNon_Weighted";
		String type = ".ser";
		HashMap<Integer, HashMap<Integer,Double>> jaccard = SerializationHelper.deserialize(path + file + type);
		
		// get avg vol, std dev
		VolatilityAnalysis va = new VolatilityAnalysis();
		va.avgVolatility(jaccard, (path + file));
	}
}