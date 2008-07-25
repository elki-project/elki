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
     * The dimensionality of the feature space (needed for serialization).
     */
    private int dimensionality;

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
     * @param dimensionality         the dimensionality of the feature space (needed for serialization)
     * @param correlationValue       the correlation dimension to be represented by the
     *                               CorrelationDistance
     * @param euklideanValue         the euclidean distance to be represented by the
     *                               CorrelationDistance
     * @param commonPreferenceVector the common preference vector of the two objects defining this distance
     */
    public PreferenceVectorBasedCorrelationDistance(int dimensionality,
                                                    int correlationValue,
                                                    double euklideanValue,
                                                    BitSet commonPreferenceVector) {
        super(correlationValue, euklideanValue);
        this.dimensionality = dimensionality;
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
     * Returns a string representation of this PreferenceVectorBasedCorrelationDistance.
     *
     * @return the correlation value, the euklidean value and the common preference vector
     *         separated by blanks
     */
    public String toString() {
        return super.toString() + " " + commonPreferenceVector.toString();
    }

    /**
     * @throws IllegalArgumentException if the dimensionality values and common preference vectors
     *                                  of this distance and the specified distance are not equal
     * @see Distance#plus(Distance)
     */
    public PreferenceVectorBasedCorrelationDistance plus(PreferenceVectorBasedCorrelationDistance distance) {
        if (this.dimensionality != distance.dimensionality) {
            throw new IllegalArgumentException("The dimensionality values of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.dimensionality     " + this.dimensionality + "\n" +
                "distance.dimensionality " + distance.dimensionality + "\n"
            );
        }

        if (!this.commonPreferenceVector.equals(distance.commonPreferenceVector)) {
            throw new IllegalArgumentException("The common preference vectors of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.commonPreferenceVector     " + this.commonPreferenceVector + "\n" +
                "distance.commonPreferenceVector " + distance.commonPreferenceVector + "\n"
            );
        }

        return new PreferenceVectorBasedCorrelationDistance(
            dimensionality,
            getCorrelationValue() + distance.getCorrelationValue(),
            getEuklideanValue() + distance.getEuklideanValue(),
            (BitSet) commonPreferenceVector.clone());
    }

    /**
     * @throws IllegalArgumentException if the dimensionality values and common preference vectors
     *                                  of this distance and the specified distance are not equal
     * @see Distance#minus(Distance)
     */
    public PreferenceVectorBasedCorrelationDistance minus(PreferenceVectorBasedCorrelationDistance distance) {
        if (this.dimensionality != distance.dimensionality) {
            throw new IllegalArgumentException("The dimensionality values of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.dimensionality     " + this.dimensionality + "\n" +
                "distance.dimensionality " + distance.dimensionality + "\n"
            );
        }

        if (!this.commonPreferenceVector.equals(distance.commonPreferenceVector)) {
            throw new IllegalArgumentException("The common preference vectors of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.commonPreferenceVector     " + this.commonPreferenceVector + "\n" +
                "distance.commonPreferenceVector " + distance.commonPreferenceVector + "\n"
            );
        }

        return new PreferenceVectorBasedCorrelationDistance(
            dimensionality,
            getCorrelationValue() - distance.getCorrelationValue(),
            getEuklideanValue() - distance.getEuklideanValue(),
            (BitSet) commonPreferenceVector.clone());
    }

    /**
     * Checks if the dimensionality values and common preference vectors
     * of this distance and the specified distance are equal.
     * If the check fails an IllegalArgumentException is thrown,
     * otherwise {@link CorrelationDistance#compareTo(CorrelationDistance)
     * CorrelationDistance#compareTo(distance)} is returned.
     *
     * @return the value of {@link CorrelationDistance#compareTo(CorrelationDistance) CorrelationDistance#compareTo(distance)}
     * @throws IllegalArgumentException if the dimensionality values and common preference vectors
     *                                  of this distance and the specified distance are not equal
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(PreferenceVectorBasedCorrelationDistance distance) {
        if (this.dimensionality != distance.dimensionality) {
            throw new IllegalArgumentException("The dimensionality values of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.dimensionality     " + this.dimensionality + "\n" +
                "distance.dimensionality " + distance.dimensionality + "\n"
            );
        }

        if (!this.commonPreferenceVector.equals(distance.commonPreferenceVector)) {
            throw new IllegalArgumentException("The common preference vectors of this distance " +
                "and the specified distance need to be equal.\n" +
                "this.commonPreferenceVector     " + this.commonPreferenceVector + "\n" +
                "distance.commonPreferenceVector " + distance.commonPreferenceVector + "\n"
            );
        }

        return super.compareTo(distance);
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.distance.CorrelationDistance#writeExternal(java.io.ObjectOutput)}
     * and writes additionally the dimensionality and each Byte of the common preference vector
     * to the specified stream.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(dimensionality);
        for (int d = 0; d < dimensionality; d++) {
            out.writeBoolean(commonPreferenceVector.get(d));
        }
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.distance.CorrelationDistance#readExternal(java.io.ObjectInput)}
     * and reads additionally the dimensionality and each Byte of the common preference vector
     * from the specified stream..
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        dimensionality = in.readInt();
        commonPreferenceVector = new BitSet();
        for (int d = 0; d < dimensionality; d++) {
            commonPreferenceVector.set(d, in.readBoolean());
        }
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 16 + 4 * dimensionality (8 Byte for two integer, 8 Byte for a double value,
     *         and 4 * dimensionality for the bit set)
     */
    public int externalizableSize() {
        return super.externalizableSize() + 4 + dimensionality * 4;
    }
}
