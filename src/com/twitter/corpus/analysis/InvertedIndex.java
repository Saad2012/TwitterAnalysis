package com.twitter.corpus.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.twitter.corpus.data.Status;
import com.twitter.corpus.data.StatusStream;
import com.twitter.corpus.demo.Admin;
import com.twitter.corpus.demo.ProcessedTweet;
import com.twitter.corpus.demo.TweetProcessor;

public class InvertedIndex {
	public InvertedIndex(){
	}
	private static final Logger LOG = Logger.getLogger(TermTermWeights.class);
		public static int counter =1;

	/**
	 * 
	 * @param StatusStream stream
	 * @return <del>HashMap<Integer, HashSet<Long>> InvertIndex</del> IndexAndDocCount
	 * <br><br>
	 * Returns the index built from a directory
	 * @throws IOException
	 */
	public HashMap<Integer, HashSet<Long>> buildIndex(StatusStream stream) throws IOException{
		int skip=0;
		if(TweetProcessor.stopwords == null){
			TweetProcessor.callStops();
		}
		// Integer -> term id, hashset of long -> doc occurances

		HashMap<Integer, HashSet<Long>> termIndex = new HashMap<Integer, HashSet<Long>>();

		int docNum=0;
		Status status;
		Long lastTime = System.currentTimeMillis();
		try {
			while ((status = stream.next()) != null)
			{
				// status 302 is a redirect, ie a retweet
				if(status.getHttpStatusCode() == 302){
					continue;
				}					

				String tweet = status.getText();
				if (tweet == null){	continue;}
				ProcessedTweet pt = TweetProcessor.processTweet(status.getText(),status.getId());

				for(int i=0; i< pt.termIdList.size() ; i++){
					if(!termIndex.containsKey(pt.termIdList.get(i)))
					{// tdh - termDocHash
						HashSet<Long> tdh = new HashSet<Long>();
						if(!tdh.contains(status.getId())){
							tdh.add(status.getId());
						}
						termIndex.put(pt.termIdList.get(i), tdh);
					}
					else
					{
						if(!termIndex.get(pt.termIdList.get(i)).contains(status.getId())){
							termIndex.get(pt.termIdList.get(i)).add(status.getId());
						}
					}
				}
				docNum++;
				if(docNum % 10000 == 0 ){
					Long currTime = System.currentTimeMillis();
					LOG.info(/*"block: "+counter+"*/ docNum + " tweets indexed in " +  Admin.getTime(lastTime, currTime));
					lastTime = currTime;
				}
				if(docNum > 50000){
					LOG.info(termIndex.size() + " total terms.");
					break;
				}
			}
			LOG.info(termIndex.size() + " total terms indexed.");
		}
		finally
		{
		}
		
		// remove index terms with df less than 8 and > ??
		// TODO Sort out tf upper threshold. Investigate weighting, currently looking at raw term frequencies over daily corpus, need refined weighting
		HashMap<Integer, HashSet<Long>> thresholdIndex = new HashMap<Integer, HashSet<Long>>((int)termIndex.size()/3);
		
		Iterator<Map.Entry<Integer, HashSet<Long>>> indexIterator = termIndex.entrySet().iterator();
		while(indexIterator.hasNext()){
			Map.Entry<Integer, HashSet<Long>> termEntry = indexIterator.next();
			// frequency threshold
//			if(termEntry.getValue().size() > 9 && termEntry.getValue().size() < 5000){
				thresholdIndex.put(termEntry.getKey(), termEntry.getValue());
//			}
		}
		return thresholdIndex;
//		return termIndex;
	}
	/**
	 * 
	 * @param index
	 * @return a mapping of terms and their tf's
	 * @throws IOException
	 */
	public HashMap<Integer, Double> getTfidf(HashMap<Integer, HashSet<Long>> index) throws IOException{
		HashMap<Integer, Double> tfidfMap = new HashMap<Integer, Double>(index.size());
		ArrayList<Double> tfidfArrayList = new ArrayList<Double>(index.size());

		Iterator<Entry<Integer, HashSet<Long>>> index2TFIterator = index.entrySet().iterator();
		int http=0;
		while(index2TFIterator.hasNext()){
			Entry<Integer, HashSet<Long>> term = index2TFIterator.next();
			if(TermTermWeights.termBimap.inverse().get(term.getKey()).contains("http")){
				http++;
			}
		}
		
		Iterator<Entry<Integer, HashSet<Long>>> indexTFIterator = index.entrySet().iterator();

		// print out  list of the low freq terms ie tf=1,2,3,4...
		BufferedWriter lowTfPtint = new BufferedWriter(new FileWriter("/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/tweetIndex/"+counter+" lowTf_.txt"));
		BufferedWriter highTfPtint = new BufferedWriter(new FileWriter("/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/tweetIndex/"+counter+" highTf.txt"));
		ArrayList<tfPair> tf = new ArrayList<tfPair>(index.size());
		BufferedWriter idfPrint = new BufferedWriter(new FileWriter("/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/tweetIndex/"+counter+" idf.txt"));
		int lowLineCnt=0;
		int highLineCnt = 0;
		while(indexTFIterator.hasNext()){
			Entry<Integer, HashSet<Long>> term = indexTFIterator.next();
			// tf lower threshold
			if(term.getValue().size() < 16){
				lowTfPtint.append(TermTermWeights.termBimap.inverse().get(term.getKey()) + ": " + term.getValue().size() + ", ");
				lowLineCnt++;
				if(lowLineCnt ==10){
					lowLineCnt = 0;
					lowTfPtint.append("\n");
				}
			}
			
			// tf upper threshold
			if(term.getValue().size() >500){
				highTfPtint.append(TermTermWeights.termBimap.inverse().get(term.getKey()) + ": " + term.getValue().size() + ", ");
				highLineCnt++;
				if(highLineCnt ==10){
					highLineCnt = 0;
					highTfPtint.append("\n");
				}
			}
			tf.add(new tfPair(term.getKey(), term.getValue().size()));
		}
		lowTfPtint.close();
		highTfPtint.close();
		
		// deal with full tf 
		Collections.sort(tf, new tfComparator2());
		Iterator<tfPair> tfIter = tf.iterator();
		while(tfIter.hasNext()){
			tfPair tfp = tfIter.next();
			idfPrint.append(TermTermWeights.termBimap.inverse().get(tfp.term) + ", " + tfp.tf.toString() + "\n");
		}
		idfPrint.close();

		// **********************************************************
		// get array of term frequencies, cosrt and print them out
//		
//		Iterator<Entry<Integer, HashSet<Long>>> indexIterator = index.entrySet().iterator();
//		ArrayList<Integer> tf = new ArrayList<Integer>(index.size());
//		while(indexIterator.hasNext()){
//			Entry<Integer, HashSet<Long>> term = indexIterator.next();
//			tf.add(term.getValue().size());
//		}
//		Collections.sort(tf, new tfComparator());
//		BufferedWriter bir = new BufferedWriter(new FileWriter("/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/tweetIndex/idf.txt"));
//		Iterator<Integer> tfArrayListIterator = tf.iterator();
//		while(tfArrayListIterator.hasNext()){
//			Integer idf = tfArrayListIterator.next();
//			bir.append(idf + ",\n");
//		}
//		bir.close();


//		// calcualte idf values
//		while(indexIterator.hasNext()){
//			Entry<Integer, HashSet<Long>> term = indexIterator.next();
//			// get IDF portion, ie terms relevance across corpus
//			Double tfidf = Math.log10(((double)TermTermWeights.docTermsMap.size()/(double)index.get(term.getKey()).size()));
//			Double roundTfIdf = (double)Math.round(tfidf * 10000) / 10000;
//			tfidfArrayList.add(roundTfIdf);
//		}
//		Collections.sort(tfidfArrayList, new idfComparator());
//
//		BufferedWriter br = new BufferedWriter(new FileWriter("/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/tweetIndex/idf.txt"));
//		Iterator<Double> tfidfArrayListIterator = tfidfArrayList.iterator();
//		while(tfidfArrayListIterator.hasNext()){
//			Double idf = tfidfArrayListIterator.next();
//			br.append(idf + ",\n");
//		}
//		br.close();
		counter++;
		return tfidfMap;
	}

	public class tfPair{
		public tfPair(Integer term, Integer tf){
			this.term = term;
			this.tf = tf;
		}
		public Integer term;
		public Integer tf;
	}

	public static class idfComparator implements Comparator<Double> {
		@Override
		public int compare(Double arg0, Double arg1) {
			// -1 to reverese the list, ie descending
			return (-1)*arg0.compareTo(arg1);
		}
	}
	public static class tfComparator implements Comparator<Integer> {
		@Override
		public int compare(Integer arg0, Integer arg1) {
			// -1 to reverese the list, ie descending
			return (-1)*arg0.compareTo(arg1);
		}
	}
	public static class tfComparator2 implements Comparator<tfPair> {
		@Override
		public int compare(tfPair arg0, tfPair arg1) {
			// -1 to reverese the list, ie descending
			return (-1)*arg0.tf.compareTo(arg1.tf);
		}
	}
}

























