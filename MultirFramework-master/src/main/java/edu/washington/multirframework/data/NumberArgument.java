package edu.washington.multirframework.data;

/**
 * A number argument is an argument with a unit
 * @author aman
 *
 */
public class NumberArgument  extends Argument{
	String unit;
	
	public NumberArgument(String name, int startOff, int endOff, String unit) {
		super(name, startOff, endOff);
		this.unit = unit;
	}
	
}
