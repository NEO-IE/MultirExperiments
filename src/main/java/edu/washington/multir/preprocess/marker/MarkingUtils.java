//sg
package edu.washington.multir.preprocess.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * This class contains code to handle the markings. Eg. Sorting, deleting, merging etc
 * @author aman
 *
 */
public class MarkingUtils {
	public static final int LINKOFFSET = 0;
	public static final int TYPEOFFSET = 1;
	
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
		 while(markingHeap.size() != 0) {
			 flatMarkings.add(markingHeap.remove());
		 }
		return flatMarkings;	
	}
	
	/**
	 * Gets a list of markings and returns 2 strings:
	 * a) Linking string
	 * b) Type String
	 * 8 9 Ship__naming__and__launching null 0.030186754 5 7 Air__China /m/01rjgp 0.8786364 10 11 Beijing /m/01914 0.8106947 1 2 Brand&|
	 * 5 7 /aviation/airline /m/01rjgp 10 11 /location/citytown /m/01914 6 7 /location/country /m/0d05w3 |
	 * @param markings
	 * @return
	 */
	public static ArrayList<String> getMarkingStrings(ArrayList<Marking> markings) {
		StringBuilder linkStringBuilder = new StringBuilder();
		StringBuilder typeStringBuilder = new StringBuilder();
		for(Marking m : markings) {
			linkStringBuilder.append(m.getLinkString() + " ");
			typeStringBuilder.append(m.getTypeString() + " ");
		}
		ArrayList<String> res = new ArrayList<String>();
		res.add(linkStringBuilder.toString());
		res.add(typeStringBuilder.toString());
		return res;
	}
	public static void listNames(ArrayList<Marking> markings) {
		for(Marking m : markings) {
			System.out.print(m.entityName + "(" + m.startOffset + ") ");
		}
		System.out.println();
		
	}
	/**
	 * Tester
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		String sentence = "Israel, 12 and Turkey, 13 have both urban population of 12bn, More than 15 of US and 100 ireland combined";
		CountryMarker cnmarker = new CountryMarker("meta/country_freebase_mapping");
		NumberMarker nm = new NumberMarker();
		ArrayList<ArrayList<Marking>> markingsList = new ArrayList<ArrayList<Marking>>();
		markingsList.add(cnmarker.mark(sentence));
		markingsList.add(nm.mark(sentence));
	//	MarkingUtils.listNames(markingsList.get(0));
	//	MarkingUtils.listNames(markingsList.get(1));
	//	MarkingUtils.listNames(MarkingUtils.mergeMarkings(markingsList));
		for(String m : MarkingUtils.getMarkingStrings(MarkingUtils.mergeMarkings(markingsList))) {
			System.out.println(m);
		}
	}

}
