//sg
/**
 * This extends the standard distant supervision, the only thing that's different is the way in which matching is done.
 * Lots of fields/methods of distant supervision were converted to protected to help with this.
 */
package edu.washington.multirframework.distantsupervision;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.data.NegativeAnnotation;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;
import edu.washington.multirframework.util.BufferedIOUtils;

public class UnitDistantSupervision extends DistantSupervision {
	
	public UnitDistantSupervision(ArgumentIdentification ai, SententialInstanceGeneration sig, RelationMatching rm, NegativeExampleCollection nec){
		super(ai, sig, rm, nec);
	}
	
	@Override
	public void run(String outputFileName,KnowledgeBase kb, Corpus c) throws SQLException, IOException, NullPointerException{
		long start = System.currentTimeMillis();
    	PrintWriter dsWriter = new PrintWriter(BufferedIOUtils.getBufferedWriter(new File(outputFileName)));
		Iterator<Annotation> di = c.getDocumentIterator();
		if(null == di) {
			System.out.println("NULL, I am going to crash before you blink");
		}
		int count =0;
		long startms = System.currentTimeMillis();
		long timeSpentInQueries = 0;
		while(di.hasNext()){
			Annotation d = di.next();
			if(null == d) {
				System.out.println(d);
			}
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);

			List<NegativeAnnotation> documentNegativeExamples = new ArrayList<>();
			List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> documentPositiveExamples = new ArrayList<>();
			for(CoreMap sentence : sentences){
				int sentGlobalID = sentence.get(SentGlobalID.class);
				
				//argument identification 
				/**
				 * Here the argument identification module is the country argument matcher, thus this is essentially
				 * just a list of the countries that are there in the sentence
				 */
				List<Argument> arguments =  ai.identifyArguments(d,sentence);
				
				//sentential instance generation
				
				//List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				List<Pair<Argument,Argument>> countryNumberPairs = sig.generateSententialInstances(arguments, sentence);
				
				//relation matching
				List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = 
						rm.matchRelations(sententialInstances,kb,sentence,d);
				
				//adding sentence IDs
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsAnnotationWithSentIDs = new ArrayList<>();
				for(Triple<KBArgument,KBArgument,String> trip : distantSupervisionAnnotations){
					Integer i = new Integer(sentGlobalID);
					Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,i);
					dsAnnotationWithSentIDs.add(p);
				}		
				//negative example annotations
				List<NegativeAnnotation> negativeExampleAnnotations =
						findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
								kb,sentGlobalID);
				
				documentNegativeExamples.addAll(negativeExampleAnnotations);
				documentPositiveExamples.addAll(dsAnnotationWithSentIDs);				
			}
			writeDistantSupervisionAnnotations(documentPositiveExamples,dsWriter);
			writeNegativeExampleAnnotations(nec.filter(documentNegativeExamples,documentPositiveExamples,kb,sentences),dsWriter);
			count++;
			if( count % 1000 == 0){
				long endms = System.currentTimeMillis();
				System.out.println(count + " documents processed");
				System.out.println("Time took = " + (endms-startms));
				startms = endms;
				System.out.println("Time spent in querying db = " + timeSpentInQueries);
				timeSpentInQueries = 0;
			}
		}
		dsWriter.close();
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
}
