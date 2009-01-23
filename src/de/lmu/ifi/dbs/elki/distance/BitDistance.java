package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.Bit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides a Distance for a bit-valued distance.
 *
 * @author Arthur Zimek
 */
public class BitDistance extends NumberDistance<BitDistance, Bit> {
    /**
     * Generated serial version UID
     */
    private static final long serialVersionUID = 6514853467081717551L;

    /**
     * Empty constructor for serialization purposes.
     */
    public BitDistance() {
        super(null);
    }

    /**
     * Constructs a new BitDistance object that represents the bit argument.
     *
     * @param bit the value to be represented by the BitDistance.
     */
    public BitDistance(boolean bit) {
        super(new Bit(bit));
    }

    public String description() {
        return "BitDistance.bitValue";
    }

    public BitDistance plus(BitDistance distance) {
        return new BitDistance(this.bitValue() || distance.bitValue());
    }

    public BitDistance minus(BitDistance distance) {
        return new BitDistance(this.bitValue() ^ distance.bitValue());
    }

    /**
     * Returns the value of this BitDistance as a boolean.
     *
     * @return the value as a boolean
     */
    public boolean bitValue() {
        return this.getValue().bitValue();
    }


    /**
     * Writes the bit value of this BitDistance to the specified stream.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(this.bitValue());
    }

    /**
     * Reads the bit value of this BitDistance from the specified stream.
     */
    public void readExternal(ObjectInput in) throws IOException {
        setValue(new Bit(in.readBoolean()));
    }

    /**
     * Returns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 1 (1 Byte for a boolean value)
     */
    public int externalizableSize() {
        return 1;
    }


}
