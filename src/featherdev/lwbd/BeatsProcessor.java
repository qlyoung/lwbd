package featherdev.lwbd;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Postprocessing functions for the beats you get from BeatDetector.
 * 
 * How convenient!
 * 
 * @author featherdev
 * 
 */
public class BeatsProcessor {

	/**
	 * Thins beats so that no two are closer together than the given time period.
	 * Prefers keeping stronger beats to weaker ones.
	 * 
	 * @param minTimeBetween
	 * @param timeIntensityBeats
	 */
	public static LinkedList<Beat> thinBeats(LinkedList<Beat> timeOrderedBeats, long minTimeBetween) {

		LinkedList<Beat> result = new LinkedList<Beat>();
		
		Iterator<Beat> iterator = timeOrderedBeats.iterator();
		Beat currentBeat, nextBeat;
		currentBeat = iterator.next();

		while ( iterator.hasNext() ) {
			
			nextBeat = iterator.next();

			// check time difference
			if (currentBeat.timeMs - nextBeat.timeMs > minTimeBetween) {
				
				// keep the stronger beat
				if (nextBeat.energy > currentBeat.energy) {
					result.add(nextBeat);
					currentBeat = nextBeat;
				}
				else {
					// make sure we aren't adding a duplicate
					if (!result.isEmpty()){
						if (result.getLast() != currentBeat)
							result.add(currentBeat);
					}
					else
						result.add(currentBeat);
				}
			}
		}
		
		return result;

	}
	/**
	 * Drops all beats below the given sound energy threshold
	 * @param beats
	 * @param threshold
	 * @return
	 */
	public static LinkedList<Beat> dropWeakBeats(LinkedList<Beat> beats, float threshold){
		LinkedList<Beat> result = new LinkedList<Beat>();
		
		for (Beat b : beats){
			if (b.energy > threshold)
				result.add(b);
		}
		
		return result;
	}
}