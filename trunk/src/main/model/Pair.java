// Copyright (c) 2010 Jessica Lundberg and Andreas Lundberg
/**
 * Author: Jessica Anderson
 * Created: March 3, 2010
 * 
 */
package model;

/** Takes two objects and pairs them in a data structure.
 * 
 * @author Jessica Anderson
 *
 */
public class Pair<T, S> {
	
	private final T first;
	private final S second;
	
	public Pair(T f, S s){ 
		first = f;
		second = s;   
	}

	/** return object T from the first position in the pair */
	public T getFirst() {
		return first;
	}

	/** return object T from the second position in the pair */
	public S getSecond() {
		return second;
	}

	/** String representation of the pair */
	@Override 
	public String toString() { 
		return "(" + first.toString() + ", " + second.toString() + ")"; 
	}
}

