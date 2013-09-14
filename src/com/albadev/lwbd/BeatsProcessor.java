package com.albadev.lwbd;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Post processor for use with arraylists returned by
 * AudioFunctions.detectPeaks(). Formats data into convenient forms.
 * 
 * @author albatross
 * 
 */
public class BeatsProcessor {

	/**
	 * Trims beats to guarantee that the specified number of milliseconds
	 * are present between each beat. Compares consecutive beats and checks
	 * time in between; if it is less than the specified amount of time then
	 * it removes the smaller beat and checks against the next consecutive
	 * values.
	 * 
	 * @param minTimeBetween
	 * @param timeIntensityBeats
	 */
	public static void removeCloseBeats(long minTimeBetween,
			LinkedHashMap<Long, Float> timeIntensityBeats) {

		Iterator<Long> iterator = timeIntensityBeats.keySet().iterator();

		long prevTime = iterator.next();
		long currTime = 0l;

		while (iterator.hasNext()) {
			currTime = iterator.next();

			if (currTime - prevTime < minTimeBetween) {

				// keep the stronger beat

				if (timeIntensityBeats.get(prevTime) < timeIntensityBeats
						.get(currTime)) {
					
					// remove the first beat
					timeIntensityBeats.remove(prevTime);
					
					iterator = timeIntensityBeats.keySet().iterator();
					prevTime = iterator.next();
					
				} else {
					// remove the second beat
					iterator.remove();
				}
			} else {
				prevTime = currTime;
			}
		}

	}

}