package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides a Distance for a double-valued distance.
 *
 * @author Elke Achtert
 */
public class DoubleDistance extends NumberDistance<DoubleDistance, Double> {

    /**
     * Generated serialVersionUID.
     */
    private static final long serialVersionUID = 3711413449321214862L;

    /**
     * Empty constructor for serialization purposes.
     */
    public DoubleDistance() {
        super(null);
    }

    /**
     * Constructs a new DoubleDistance object that represents the double
     * argument.
     *
     * @param value the value to be represented by the DoubleDistance.
     */
    public DoubleDistance(double value) {
        super(value);
    }

    public String description() {
        return "DoubleDistance.doubleValue";
    }

    public DoubleDistance plus(DoubleDistance distance) {
        return new DoubleDistance(this.getValue() + distance.getValue());
    }

    public DoubleDistance minus(DoubleDistance distance) {
        return new DoubleDistance(this.getValue() - distance.getValue());
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * distance.
     *
     * @param distance the distance to be multiplied with this distance
     * @return a new distance as the product of this distance and the given
     *         distance
     */
    public DoubleDistance times(DoubleDistance distance) {
        return new DoubleDistance(this.getValue() * distance.getValue());
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * double value.
     *
     * @param lambda the double value this distance should be multiplied with
     * @return a new distance as the product of this distance and the given
     *         double value
     */
    public DoubleDistance times(double lambda) {
        return new DoubleDistance(this.getValue() * lambda);
    }

    /**
     * Writes the double value of this DoubleDistance to the specified stream.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(this.getValue());
    }

    /**
     * Reads the double value of this DoubleDistance from the specified stream.
     */
    public void readExternal(ObjectInput in) throws IOException {
        setValue(in.readDouble());
    }

    /**
     * Returns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 8 (8 Byte for a double value)
     */
    public int externalizableSize() {
        return 8;
    }
}
