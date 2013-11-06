package com.albadev.lwbd;

/***
 * Represents a single rhythmic onset at a discrete point in time,
 * with a discrete sound energy in the range [0, 1]
 * @author albatross
 *
 */

public class Beat implements Cloneable {

	/***
	 * this beat's in-song time in milliseconds
	 */
	public final long timeMs;
	/***
	 * this beat's sound energy (normalized to range [0, 1])
	 */
	public final float energy;

	public Beat(long timeMs, float energy){
		this.timeMs = timeMs;
		this.energy = energy;
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		String str = "time: " + timeMs + "\n"; 
		str += "energy: " + energy;
		return str;
	}

}