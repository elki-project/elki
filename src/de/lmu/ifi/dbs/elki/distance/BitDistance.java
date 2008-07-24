package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.Bit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * TODO arthur comment
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

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#description()
     */
    public String description() {
        return "BitDistance.bitValue";
    }

    /**
     * @see Distance#plus(Distance)
     */
    public BitDistance plus(BitDistance distance) {
        return new BitDistance(this.isBit() || distance.isBit());
    }

    /**
     * @see Distance#minus(Distance)
     */
    public BitDistance minus(BitDistance distance) {
        return new BitDistance(this.isBit() ^ distance.isBit());
    }

    public boolean isBit() {
        return this.value.bitValue();
    }


    /**
     * Writes the bit value of this BitDistance to the specified stream.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(this.isBit());
    }

    /**
     * Reads the bit value of this BitDistance from the specified stream.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException {
        this.value = new Bit(in.readBoolean());
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 1 (1 Byte for a boolean value)
     * @see Distance#externalizableSize()
     */
    public int externalizableSize() {
        return 1;
    }


}
