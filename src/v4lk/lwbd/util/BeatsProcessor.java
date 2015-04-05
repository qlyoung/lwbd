package v4lk.lwbd.util;

import java.util.ArrayList;

/**
 * Postprocessing functions for the beats you get from BeatDetector.
 * How convenient!
 * 
 * @author Quentin Young
 */
public class BeatsProcessor {

    /**
     * Thins beats so that consecutive beats are separated by
     * at least <gapMs> milliseconds. Comparison is by sound
     * energy.
     *
     * @param beats array of Beat objects, sorted by time
     * @param gapMs minimum time between beats in milliseconds
     */
    public static Beat[] thinBeats(Beat[] beats, long gapMs) {
        // thin and store survivors to list
        ArrayList<Beat> temp = new ArrayList<Beat>();
        Beat currBeat, prevBeat = beats[0];
        for (int i = 1; i < beats.length; i++) {
            currBeat = beats[i];
            if (currBeat.timeMs - prevBeat.timeMs >= gapMs)
                temp.add(currBeat);
            else {
                Beat winner = currBeat.energy > prevBeat.energy ? currBeat : prevBeat;
                int index = temp.size() - 1;
                temp.set(index, winner);
            }
        }

        return temp.toArray(new Beat[temp.size()]);
    }
    /**
     * Filters beats by sound energy.
     *
     * @param beats array of Beat objects
     * @param minimum minimum sound energy [0..1]
     * @param maximum maximum sound energy [0..1]
     * @return
     */
    public static Beat[] filterByEnergy(Beat[] beats, float minimum, float maximum){
        ArrayList<Beat> result = new ArrayList<Beat>();

        for (Beat b : beats){
            if (b.energy > minimum && b.energy < maximum)
                result.add(b);
        }

        return result.toArray(new Beat[result.size()]);
    }
}