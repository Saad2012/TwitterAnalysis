package com.twitter.corpus.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.twitter.corpus.demo.TweetAnalysis;

public class Jaccard {
	private static final Logger LOG = Logger.getLogger(Jaccard.class);
	public Jaccard(int size){
		jaccardList = new HashMap<Integer, HashMap<Integer,Double>>(size);
	}
	// dayCounter is used for jaccard map, also printed out on entry to jaccard calculator
	private static int dayCounter = 0;
	private static int unionZero = 0;
	private static int termError = 0;
	public static HashMap<Integer, HashMap<Integer,Double>> jaccardList;

	/*** 
	 * 
	 * @param feed array of cosets for "today" and "yesterday"
	 * @return maintains a static hashMap of jaccards for each term over duration of corpus, access when finished traversing the corpus
	 * @throws IOException 
	 */
	public static void getJaccardSimilarity(ArrayList<HashMap<Integer, HashMap<Integer, Double>>> cosetArray) throws IOException{
		LOG.info("Interval: " + dayCounter);
		// Get the union of the keys from both arrays
		ArrayList<HashMap<Integer, HashMap<Integer, Double>>> copyMap = new ArrayList<HashMap<Integer,HashMap<Integer, Double>>>(cosetArray);
		// TODO coset array contained somewhere 4370, think all entries are null and so size is zero... . why??
		// union set is set of terms(keys) that occur in both hashsets
		// incorrect: union set is set of outer keys, not correlates
		// need to get innet set of correlates to get the union
		Set<Integer> allTerms = new HashSet<Integer>(copyMap.get(0).keySet());
		//TODO BIGGEST TODO. ON THE PLANET! sort jaccarding to only take top 5 coweighted terms! 
		Set<Integer> interSet = null;
		Set<Integer> unionSet = null;
		allTerms.addAll(copyMap.get(1).keySet());
		copyMap = null;
		
		// now have all unique keys and can iterate
		Iterator<Integer> termIterator = allTerms.iterator();
		while(termIterator.hasNext()){
			Integer term = termIterator.next();

			try{
				interSet = new HashSet<Integer>(cosetArray.get(0).get(term).size());

				HashMap<Integer, Double> termCosetA = cosetArray.get(0).get(term);	// grab term correlates from both hashmaps
				HashMap<Integer, Double> termCosetB = cosetArray.get(1).get(term);

				int unionInit = 0;
				if( termCosetA != null ) { unionInit += termCosetA.size(); }
				if(termCosetB!=null){ unionInit += termCosetB.size(); }

				unionSet = new HashSet<Integer>(unionInit);
				if(termCosetA != null){ unionSet.addAll(termCosetA.keySet()); }
				if(termCosetB != null){	unionSet.addAll(termCosetB.keySet()); }

				// if term doesn't occur on particular day (unlightly, given thresholds performed already), termCoset will be null, this would give a 0 jaccard
				if(termCosetB != null && termCosetA != null){
					// iterate A, get coweight and check i
					Iterator<Integer> aIter = termCosetA.keySet().iterator();
					while(aIter.hasNext()){
						Integer t = aIter.next();
						if(termCosetB.containsKey(t)){
							interSet.add(t);
						}
					}
					termCosetB.keySet().removeAll(interSet);	// remove all keys that have already been detected
					Iterator<Integer> iterB = termCosetB.keySet().iterator();
					while(iterB.hasNext()){
						Integer t = iterB.next();
						if(termCosetA.containsKey(t)){
							interSet.add(t);
						}
					}
					double jaccard = 0.0;
					if(unionSet.size() == 0){ unionZero++;}
					if(interSet.size() > 0 && unionSet.size() > 0){
						jaccard = (double)interSet.size() / (double) unionSet.size();	// switch to union
					}
					else{
						jaccard = 0.0; 	// set jaccard = 0 as a placeholder, for diffing want to check agains zero
					}

					if(!jaccardList.containsKey(term)){
						HashMap<Integer, Double> jEachDayMap = new HashMap<Integer, Double>(33);	// 33 intervals
						jEachDayMap.put(dayCounter, jaccard);
						jaccardList.put(term, jEachDayMap);
					}
					else{
						jaccardList.get(term).put(dayCounter, jaccard);
					}
				}
				else{
					// else one of set a or b is null...
					termError++;
					LOG.info("termCosetA or termCosetB is null.");
				}
			}
			catch(NullPointerException np){
				termError++;
				LOG.info("NullPointerException at Line 102 Jaccard, a term is not present one of the days");
			}
		}
		LOG.info("Term error occured " + termError + "times." );
		dayCounter++; // must be incremented here to ensure continuity
	}
	
	public static void printResults() throws IOException{
		BufferedWriter out = new BufferedWriter(new FileWriter(TweetAnalysis.jaccardOutput));
		Iterator<Entry<Integer, HashMap<Integer, Double>>> termIter = jaccardList.entrySet().iterator();
		while(termIter.hasNext()){
			Entry<Integer, HashMap<Integer, Double>> termJacc = termIter.next();
			StringBuffer sb = new StringBuffer();
			sb.append(TermTermWeights.termBimap.inverse().get(termJacc.getKey()) + " { ");
			 
			Iterator<Entry<Integer, Double>> jaccIter = termJacc.getValue().entrySet().iterator();
			while(jaccIter.hasNext()){
				Entry<Integer, Double> entry = jaccIter.next();
				double val = (double)Math.round(entry.getValue() * 1000) / 1000;
				sb.append(entry.getKey().toString() + " = " + val + ", ");
			}
			out.write(sb.replace(sb.length()-2, sb.length()-2," }\n").toString());
		}
		out.close();
	}
}