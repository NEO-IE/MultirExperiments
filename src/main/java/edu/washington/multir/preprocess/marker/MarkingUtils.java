//sg
package edu.washington.multir.preprocess.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * This class contains code to handle the markings. Eg. Sorting, deleting, merging etc
 * @author aman
 *
 */
public class MarkingUtils {
	
	/**
	 * This function merges the marking on the basis of the start offset attribute
	 * Basically, takes a list of matchings and flattens it to a large list
	 * @return a flattened list of matchings
	 */
	public static ArrayList<Marking> mergeMarkings(ArrayList< ArrayList < Marking > > markingsList) {
		
		PriorityQueue<Marking> markingHeap = new PriorityQueue<Marking>();
		//Just go over the matchings and fill them in a min heap, finally get them out
		for(ArrayList<Marking> markings : markingsList) {
			for(Marking marking : markings) {
				markingHeap.add(marking);
			}
		}
		ArrayList<Marking> flatMarkings = new ArrayList<Marking>();
		 Iterator<Marking> heapIter = markingHeap.iterator();
		 while(heapIter.hasNext()) {
			 flatMarkings.add(heapIter.next());
		 }
		return flatMarkings;	
	}
	
	public static void main(String args[]) throws IOException {
		String sentence = "Israel, 12 and India, 13 have both urban population of 12bn";
		CountryMarker cnmarker = new CountryMarker("meta/country_freebase_mapping");
		NumberMarker nm = new NumberMarker();
		ArrayList<ArrayList<Marking>> markingsList = new ArrayList<ArrayList<Marking>>();
		markingsList.add(cnmarker.mark(sentence));
		markingsList.add(nm.mark(sentence));
		System.out.println(MarkingUtils.mergeMarkings(markingsList));
		
	}

}
