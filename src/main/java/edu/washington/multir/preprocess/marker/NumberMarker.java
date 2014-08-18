package edu.washington.multir.preprocess.marker;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberMarker implements Marker {

	@Override
	public ArrayList<Marking> mark(String sentence) {
		Pattern p = Pattern.compile("-?\\d+");
		ArrayList<Marking> numbers = new ArrayList<Marking>();
		String words[] = sentence.split(" ");
		int i = 0;
		for(String word : words) {
			Matcher m = p.matcher(word);
			if(m.find()) {
				numbers.add(new Marking(i, i + 1, m.group(), m.group(), 1, Marking.NUMBER));
			}
			i++;
		}
		return numbers;
	}
	public static void main(String args[]) {
		NumberMarker nm = new NumberMarker();
		System.out.println(nm.mark("12 The GDP Of India is expected to grow by over 100$ per year"));
	}

}
