package com.twitter.corpus.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.twitter.corpus.analysis.CosetSerializer;
import com.twitter.corpus.analysis.InvertedIndex;
import com.twitter.corpus.analysis.Jaccard;
import com.twitter.corpus.analysis.TermTermWeights;
import com.twitter.corpus.data.HtmlStatusCorpusReader;
import com.twitter.corpus.data.StatusStream;
import com.twitter.corpus.types.CoWeight;

public class TweetAnalysis{
	private static final Logger LOG = Logger.getLogger(TweetAnalysis.class);
	public static String output;
	public static String toolsDir;
	private TweetAnalysis() {}
	private static final String INPUT_OPTION = "input";
	private static final String OUTPUT_OPTION = "output";
	private static final String TOOLS = "tools";
	private static final String LOWER_DAILY_THRESH = "gt";
	private static final String UPPER_DAILY_THRESH = "lt";
	private static final String TERM_CORRELATION_THRESH = "m";
	private static final String COSET_TERMS = "t";
	
	public static int lowCutoffGlobal = 2;
	public static HashMap<Integer, HashSet<Long>> corpusIndex;
	private static ArrayList<HashMap<Integer, HashSet<Long>>> intervalIndices;
	private static int indexDocCount=0;

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("input directory or file").create(INPUT_OPTION));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("index location").create(OUTPUT_OPTION));
		options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("stopwords").create(TOOLS));
		options.addOption(OptionBuilder.withArgName("constant").hasArg().withDescription("lower daily term threshold").create(LOWER_DAILY_THRESH));
		options.addOption(OptionBuilder.withArgName("constant").hasArg().withDescription("upper daily term threshold").create(UPPER_DAILY_THRESH));
		options.addOption(OptionBuilder.withArgName("constant").hasArg().withDescription("term correlation minimum").create(TERM_CORRELATION_THRESH));
		options.addOption(OptionBuilder.withArgName("constant").hasArg().withDescription("coset top terms").create(COSET_TERMS));

		CommandLine cmdline = null;
		CommandLineParser parser = new GnuParser();
		try {
			cmdline = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Error parsing command line: " + exp.getMessage());
			System.exit(-1);
		}

		if (!(cmdline.hasOption(INPUT_OPTION) && cmdline.hasOption(OUTPUT_OPTION) && cmdline.hasOption(TOOLS))) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(TweetAnalysis.class.getName(), options);
			System.exit(-1);
		}

		int cnt=0;
		int termCosetCounter=0;
		output = cmdline.getOptionValue(OUTPUT_OPTION);
		toolsDir = cmdline.getOptionValue(TOOLS);
		String rootBase = cmdline.getOptionValue(INPUT_OPTION);
		int lowerFreq = Integer.parseInt(cmdline.getOptionValue(LOWER_DAILY_THRESH));
		int upperFreq = Integer.parseInt(cmdline.getOptionValue(UPPER_DAILY_THRESH));
		double m = Double.parseDouble(cmdline.getOptionValue(TERM_CORRELATION_THRESH));
		int cosetTopN = Integer.parseInt(cmdline.getOptionValue(COSET_TERMS));

		String root = rootBase + "/20110";
		String[] filePaths = {root + "123", root + "123a", root + "124", root + "124a", root + "125", root + "125a", 
				root + "126", root + "126a", root + "127", root + "127a", root + "128", root + "128a",
				root + "129", root + "129a", root + "130", root + "130a", root + "131", root + "131a",
				root + "201", root + "201a", root + "202", root + "202a", root + "203", root + "203a",
				root + "204", root + "204a", root + "205", root + "205a", root + "206", root + "206a", 
				root + "207", root + "207a", root + "208"};

		corpusIndex = new HashMap<Integer, HashSet<Long>>(10000);

		HashMap<Integer, ArrayList<CoWeight>> blockCoSet = null;
		Jaccard initJMap = null;
		ArrayList<HashMap<Integer, ArrayList<CoWeight>>> corpusCoSetArray = new ArrayList<HashMap<Integer, ArrayList<CoWeight>>>(2);
//		HashMap<Integer, HashSet<Long>> intervalTermIndex = null;

		int corpSize=0;int docCount=0;
		intervalIndices = new ArrayList<HashMap<Integer,HashSet<Long>>>(33);
		
		
		for(String path : filePaths){
			LOG.info("Stream number : " + (cnt+1) + "\t. Indexing " + path);
			StatusStream stream = null;	FileSystem fs = FileSystem.get(new Configuration());Path file = new Path(path);
			if (!fs.exists(file)) {	System.err.println("Error: " + file + " does not exist!"); System.exit(-1);}
			if (fs.getFileStatus(file).isDir()) {stream = new HtmlStatusCorpusReader(file, fs);	}

			// 1.0 build index
			InvertedIndex ii = new InvertedIndex();
			HashMap<Integer, HashSet<Long>> intervalTermIndex = ii.buildIndex(stream);
			corpSize = corpusIndex.size();
			
			//add interval index (both old and new terms with their document sets) 
			InvertedIndex.mergeLocalIndex(intervalTermIndex);
			intervalIndices.add(intervalTermIndex);
			
			docCount = InvertedIndex.getDocCount(corpusIndex);
			cnt++;
		}
		LOG.info("Corpus index terms: " + corpusIndex.size() + " " + ( corpusIndex.size() - corpSize) + " terms added. " + (docCount - indexDocCount) + " term-document occurences.");
		
		// trim all local indexes in the array
		ArrayList<HashMap<Integer, HashSet<Long>>> trimmedLocalIndexArray = InvertedIndex.trimLocalIndices(intervalIndices, lowerFreq, upperFreq);		
		
		for(int i = 0; i < trimmedLocalIndexArray.size(); i++){
			// 2.0 calculate term cosets
			TermTermWeights ill = new TermTermWeights(trimmedLocalIndexArray.get(i) /*intervalTermIndex*/);
			blockCoSet = ill.termCosetBuilder(m);

			// 2.1 serialize term cosets
			CosetSerializer.cosetSerializer(blockCoSet, output, (termCosetCounter + 1));
			corpusCoSetArray.add(blockCoSet);			// add coset of particular day to array

			if(corpusCoSetArray.size() == 2){	// only skipped once at the start
				if(initJMap == null){			// one time initializer
					initJMap = new Jaccard(trimmedLocalIndexArray.get(i).size());	// init size plus 10% for wiggle
				}
				//				// 3.0 do the deed
				Jaccard.getJaccardSimilarity(corpusCoSetArray, cosetTopN);
				Jaccard.getJaccardEnhancedSimilarity(corpusCoSetArray, cosetTopN);
				//				// swap positions, makes our life easier
				Collections.swap(corpusCoSetArray, 0, 1);
				//				// remove the first coset array
				corpusCoSetArray.remove(1);
				termCosetCounter++;
			}
		}

		// print frequency range
		InvertedIndex.printFrequencies(TweetAnalysis.corpusIndex, output + "freqs.txt");

		TermTermWeights.serializeTermBimap(output + "/termbimap.ser");		
		Jaccard.serializeJaccards(output);
		InvertedIndex.globalIndexSerialize(corpusIndex, output);
		InvertedIndex.localIndexArraySerialize(intervalIndices, output);		
	}
}