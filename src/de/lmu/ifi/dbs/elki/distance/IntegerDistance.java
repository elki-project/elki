package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Arthur Zimek
 */
public class IntegerDistance extends NumberDistance<IntegerDistance, Integer> {
    /**
     * Created serial version UID.
     */
    private static final long serialVersionUID = 5583821082931825810L;

    /**
     * Empty constructor for serialization purposes.
     */
    public IntegerDistance() {
        super(null);
    }

    public IntegerDistance(int value) {
        super(value);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#description()
     */
    public String description() {
        return "IntegerDistance.distanceValue";
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#minus(de.lmu.ifi.dbs.elki.distance.Distance)
     */
    public IntegerDistance minus(IntegerDistance distance) {
        return new IntegerDistance(this.value - distance.value);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#plus(de.lmu.ifi.dbs.elki.distance.Distance)
     */
    public IntegerDistance plus(IntegerDistance distance) {
        return new IntegerDistance(this.value + distance.value);
    }

    /**
     * Writes the integer value of this IntegerDistance to the specified stream.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(value);
    }

    /**
     * Reads the integer value of this IntegerDistance from the specified stream.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException {
        value = in.readInt();
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 4 (4 Byte for an integer value)
     * @see Distance#externalizableSize()
     */
    public int externalizableSize() {
        return 4;
    }

}
