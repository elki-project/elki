package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;

/**
 * A PreferenceVectorBasedCorrelationDistance holds additionally to the
 * CorrelationDistance the common preference vector of
 * the two objects defining the distance.
 *
 * @author Elke Achtert
 */
public class PreferenceVectorBasedCorrelationDistance
    extends CorrelationDistance<PreferenceVectorBasedCorrelationDistance> {

    /**
     * The common preference vector of the two objects defining this distance.
     */
    private BitSet commonPreferenceVector;

    /**
     * Empty constructor for serialization purposes.
     */
    public PreferenceVectorBasedCorrelationDistance() {
        super();
    }

    /**
     * Constructs a new CorrelationDistance object.
     *
     * @param correlationValue       the correlation dimension to be represented by the
     *                               CorrelationDistance
     * @param euklideanValue         the euclidean distance to be represented by the
     *                               CorrelationDistance
     * @param commonPreferenceVector the common preference vector of the two objects defining this distance
     */
    public PreferenceVectorBasedCorrelationDistance(int correlationValue,
                                                    double euklideanValue,
                                                    BitSet commonPreferenceVector) {
        super(correlationValue, euklideanValue);
        this.commonPreferenceVector = commonPreferenceVector;
    }

    /**
     * Returns the common preference vector of
     * the two objects defining this distance.
     *
     * @return the common preference vector
     */
    public BitSet getCommonPreferenceVector() {
        return commonPreferenceVector;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.Distance#description()
     */
    public String description() {
        return "PreferenceVectorBasedCorrelationDistance.correlationValue, " +
            "PreferenceVectorBasedCorrelationDistance.euklideanValue, " +
            "PreferenceVectorBasedCorrelationDistance.commonPreferenceVector";
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return super.toString() + " " + commonPreferenceVector.toString();
    }

    /**
     * @see Distance#plus(Distance)
     */
    public PreferenceVectorBasedCorrelationDistance plus(PreferenceVectorBasedCorrelationDistance distance) {
        // todo
        return new PreferenceVectorBasedCorrelationDistance(getCorrelationValue() + distance.getCorrelationValue(),
            getEuklideanValue() + distance.getEuklideanValue(),
            new BitSet());
    }

    /**
     * @see Distance#minus(Distance)
     */
    public PreferenceVectorBasedCorrelationDistance minus(PreferenceVectorBasedCorrelationDistance distance) {
        // todo
        return new PreferenceVectorBasedCorrelationDistance(getCorrelationValue() - distance.getCorrelationValue(),
            getEuklideanValue() - distance.getEuklideanValue(),
            new BitSet());
    }

    /**
     * @throws UnsupportedOperationException
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(PreferenceVectorBasedCorrelationDistance o) {
        // todo
        return super.compareTo(o);
    }

    /**
     * Writes the correlation value and the euklidean value
     * of this CorrelationDistance to the specified stream.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // todo
        super.writeExternal(out);
    }

    /**
     * The object implements the readExternal method to restore its contents by
     * calling the methods of DataInput for primitive types and readObject for
     * objects, strings and arrays. The readExternal method must read the values
     * in the same sequence and with the same types as were written by
     * writeExternal.
     *
     * @param in the stream to read data from in order to restore the object
     * @throws java.io.IOException    if I/O errors occur
     * @throws ClassNotFoundException If the class for an object being restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // todo
        super.readExternal(in);
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 12 (4 Byte for an integer, 8 Byte for a double value)
     */
    public int externalizableSize() {
        // todo
        return super.externalizableSize();
    }
}
