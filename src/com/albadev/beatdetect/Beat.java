package com.albadev.beatdetect;

/***
 * Represents a single rhythmic onset at a discrete point in time,
 * with a discrete sound energy in the range [0, 1]
 * @author albatross
 *
 */

public class Beat implements Cloneable {


	public final long timeMs;
	public final float energy;


	public Beat(long timeMs, float energy){
		this.timeMs = timeMs;
		this.energy = energy;
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}


}