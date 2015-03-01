package edu.washington.multirframework.multiralgorithm;

import java.util.Random;

public interface Dataset<T> {
	
	public int numDocs();
	
	public void shuffle(Random random);
	
	public T next();
	
	public boolean next(T doc);
	
	public void reset();
}
