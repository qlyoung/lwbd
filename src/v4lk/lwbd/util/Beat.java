package v4lk.lwbd.util;

/***
 * Represents a single rhythmic onset at a discrete point in time,
 * with a discrete sound energy in the range [0, 1]
 * @author featherdev
 *
 */

public class Beat implements Cloneable {

    /**
     * the millisecond that beat occurs in the song
     */
    public final long timeMs;
    /**
     * beat energy, normalized to [0..1]
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
        int totalSeconds = (int) (timeMs / 1000f);
        int minutes = (int) (totalSeconds / 60f);
        int seconds = totalSeconds - (60 * minutes);

        return "energy " + String.format("%.2f", energy) + " @ " + minutes + ":" + seconds;
    }

}