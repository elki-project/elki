package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides a Distance for a float-valued distance.
 *
 * @author Elke Achtert
 */
public class FloatDistance extends NumberDistance<FloatDistance, Float> {

    /**
     * Generated serialVersionUID.
     */
    private static final long serialVersionUID = -5702250266358369075L;

    /**
     * Empty constructor for serialization purposes.
     */
    public FloatDistance() {
        super(null);
    }

    /**
     * Constructs a new FloatDistance object that represents the float argument.
     *
     * @param value the value to be represented by the FloatDistance.
     */
    public FloatDistance(float value) {
        super(value);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#description()
     */
    public String description() {
        return "FloatDistance.distanceValue";
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#plus(Distance)
     */
    public FloatDistance plus(FloatDistance distance) {
        return new FloatDistance(this.value + distance.value);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#minus(Distance)
     */
    public FloatDistance minus(FloatDistance distance) {
        return new FloatDistance(this.value - distance.value);
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * distance.
     *
     * @param distance the distancce to be multiplied with this distance
     * @return a new distance as the product of this distance and the given
     *         distance
     */
    public FloatDistance times(FloatDistance distance) {
        return new FloatDistance(this.value * distance.value);
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * float value.
     *
     * @param lambda the float value this distance should be multiplied with
     * @return a new distance as the product of this distance and the given
     *         double value
     */
    public FloatDistance times(float lambda) {
        return new FloatDistance(this.value * lambda);
    }

    /**
     * Writes the float value of this FloatDistance to the specified stream.
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeFloat(value);
    }

    /**
     * Reads the float value of this FloatDistance from the specified stream.
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        value = in.readFloat();
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 4 (4 Byte for a float value)
     * @see Distance#externalizableSize() 
     */
    public int externalizableSize() {
        return 4;
    }
}
