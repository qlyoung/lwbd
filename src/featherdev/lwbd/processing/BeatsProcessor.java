package featherdev.lwbd.processing;

import java.util.LinkedList;
import java.util.ListIterator;

import featherdev.lwbd.Beat;

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
	 * @param gapMs minimum time between beats in milliseconds
	 */
	public static LinkedList<Beat> thinBeats(LinkedList<Beat> timeOrderedBeats, long gapMs) {
		LinkedList<Beat> result = new LinkedList<Beat>();

		ListIterator<Beat> iterator = timeOrderedBeats.listIterator();
		result.add(iterator.next());
		
		while(iterator.hasNext()){
			Beat currBeat = iterator.next();
			Beat prevBeat = result.getLast();
		
			if (currBeat.timeMs - prevBeat.timeMs >= gapMs)
				result.add(currBeat);
			else {
				Beat winner = currBeat.energy > prevBeat.energy ? currBeat : prevBeat;
				int index = result.size() - 1;
				result.set(index, winner);
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