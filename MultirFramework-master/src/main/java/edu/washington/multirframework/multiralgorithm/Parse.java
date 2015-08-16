package edu.washington.multirframework.multiralgorithm;

import java.util.Arrays;

public class Parse {

	public int[] Y;
	public int[] Z;
	public double score;
	public MILDocument doc;
	public double[] scores; // for each relation
	public double[][] allScores;
	
	public Parse() {}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(Y);
		result = prime * result + Arrays.hashCode(Z);
		result = prime * result + Arrays.hashCode(allScores);
		result = prime * result + ((doc == null) ? 0 : doc.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(scores);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parse other = (Parse) obj;
		if (!Arrays.equals(Y, other.Y))
			return false;
		if (!Arrays.equals(Z, other.Z))
			return false;
		if (!Arrays.deepEquals(allScores, other.allScores))
			return false;
		if (doc == null) {
			if (other.doc != null)
				return false;
		} else if (!doc.equals(other.doc))
			return false;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		if (!Arrays.equals(scores, other.scores))
			return false;
		return true;
	}
}
