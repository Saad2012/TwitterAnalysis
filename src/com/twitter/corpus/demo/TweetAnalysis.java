package com.twitter.corpus.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.twitter.corpus.analysis.InvertedIndex;
import com.twitter.corpus.analysis.Jaccard;
import com.twitter.corpus.analysis.TermTermWeights;
import com.twitter.corpus.data.HtmlStatusCorpusReader;
import com.twitter.corpus.data.StatusStream;
import com.twitter.corpus.types.CoWeight;

public class TweetAnalysis{
	private static final Logger LOG = Logger.getLogger(IndexStatuses.class);
	private TweetAnalysis() {}
	//	private static final String INPUT_OPTION = "input";
	//	private static final String INDEX_OPTION = "index";
	//	private static final String HTML_MODE = "html";
	//	private static final String JSON_MODE = "json";

	public static void main(String[] args) throws Exception {
//		System.out.println("Classpath = " + System.getProperty("java.class.path"));

		String root = "/home/dock/Documents/IR/DataSets/lintool-twitter-corpus-tools-d604184/html/20110";
		String[] filePaths = {root + "123", root + "124", root + "125", root + "126", root + "127", root + "128",
							  root + "129", root + "130", root + "131", root + "201", root + "202", root + "203",
							  root + "204", root + "205", root + "206",	root + "207", root + "208"};
//		String[] filePaths = {root + "123"};
//		String[] filePaths = {root + "123", root + "123a", root + "124", root + "124a", root + "125", root + "125a", 
//				  root + "126", root + "126a", root + "127", root + "127a", root + "128", root + "128a",
//				  root + "129", root + "129a", root + "130", root + "130a", root + "131", root + "131a",
//				  root + "201", root + "201a", root + "202", root + "202a", root + "203", root + "203a",
//				  root + "204", root + "204a", root + "205", root + "205a", root + "206", root + "206a", 
//				  root + "207", root + "207a", root + "208", root + "208a"};
		//		File indexLocation = new File(cmdline.getOptionValue(INDEX_OPTION));

//		int cnt=0;
		HashMap<Integer, HashSet<CoWeight>> blockCoSet = null;
		ArrayList<HashMap<Integer, HashSet<CoWeight>>> corpusCoSetArray = new ArrayList<HashMap<Integer, HashSet<CoWeight>>>(2);
		for(String path : filePaths){
			LOG.info("Indexing " + path);
			StatusStream stream = null;
			FileSystem fs = FileSystem.get(new Configuration());
			
			Path file = new Path(path);
			
			if (!fs.exists(file)) {
				System.err.println("Error: " + file + " does not exist!");
				System.exit(-1);
			}
			if (fs.getFileStatus(file).isDir()) {
				stream = new HtmlStatusCorpusReader(file, fs);
			}		
			
			// build index
			InvertedIndex ii = new InvertedIndex();
			HashMap<Integer, HashSet<Long>> termIndex = ii.buildIndex(stream);			
			
			// calculate term-term weights
			TermTermWeights ill = new TermTermWeights(termIndex);
			blockCoSet = ill.termCosetBuilder();
			
			// prints either day by day or one day... check and fix
//			OutTermCosets.printDayByDay(blockCoSet);
			
			// calculate idf for each term
//			HashMap<Integer, Double> tfidf = ii.getTfidf(termIndex);
			termIndex =null;
			// print out tfidf graph
			
			// add coset of particular day to array
			corpusCoSetArray.add(blockCoSet);
			if(corpusCoSetArray.size() == 2){
				HashMap<Integer, Double> abc = Jaccard.getJaccardSimilarity(corpusCoSetArray);
				corpusCoSetArray.remove(0);
				corpusCoSetArray.add(0, corpusCoSetArray.get(1));
				corpusCoSetArray.remove(1);
			}
			Thread.sleep(50000);
//			cnt++;
		}
		
		Jaccard jac = new Jaccard();
//		jac.getJaccardSimilarity();
		
//		OutTermCosets.printDayByDay(corpusCoSetArray);

		// output term trends, with static print method. prints term with list of correlates and weight		
//		OutTermCosets.printDayByDay(corpusCoSetArray);
		// this is now broken, it is supposed to print out coset from one array.... outdated now since i added support for processing all together
//		OutTermCosets.printTermCosets(corpusCoSetArray);
	}
}