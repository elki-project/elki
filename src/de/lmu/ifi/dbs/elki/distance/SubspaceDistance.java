package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * The SubspaceDistance is a special distance that indicates the
 * dissimilarity between subspaces of equal dimensionality. The SubspaceDistance
 * beween two points is a pair consisting of the distance between the two subspaces
 * spanned by the strong eigenvectors of the two points and the affine distance
 * between the two subspaces.
 *
 * @author Elke Achtert
 */
public class SubspaceDistance<D extends SubspaceDistance<D>> extends AbstractDistance<D> {
    /**
     * Indicates a separator.
     */
    public static final Pattern SEPARATOR = Pattern.compile("x");

    /**
     * The subspace distance.
     */
    private double subspaceDistance;

    /**
     * The affine distance.
     */
    private double affineDistance;

    /**
     * Empty constructor for serialization purposes.
     */
    public SubspaceDistance() {
        // for serialization
    }

    /**
     * Constructs a new SubspaceDistance object.
     *
     * @param subspaceDistance the subspace distance
     * @param affineDistance   the affine distance
     */
    public SubspaceDistance(double subspaceDistance, double affineDistance) {
        this.subspaceDistance = subspaceDistance;
        this.affineDistance = affineDistance;
    }

    /**
     * @see Distance#plus(de.lmu.ifi.dbs.elki.distance.Distance)
     */
    @SuppressWarnings("unchecked")
    public D plus(D distance) {
        return (D) new SubspaceDistance<D>(this.subspaceDistance + distance.subspaceDistance,
            this.affineDistance + distance.affineDistance);
    }

    /**
     * @see Distance#minus(de.lmu.ifi.dbs.elki.distance.Distance)
     */
    @SuppressWarnings("unchecked")
    public D minus(D distance) {
        return (D) new SubspaceDistance<D>(this.subspaceDistance - distance.subspaceDistance,
            this.affineDistance - distance.affineDistance);
    }

    /**
     * @see Distance#description()
     */
    public String description() {
        return "SubspaceDistance.subspaceDistance SubspaceDistance.affineDistance";
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(D other) {

        if (this.subspaceDistance < other.subspaceDistance) {
            return -1;
        }
        if (this.subspaceDistance > other.subspaceDistance) {
            return +1;
        }
        return Double.compare(this.affineDistance, other.affineDistance);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return Double.toString(subspaceDistance) + " " + Double.toString(affineDistance);
    }

    /**
     * Returns true if o is of the same class as this instance
     * and <code>this.compareTo(o)</code> is 0,
     * false otherwise.
     *
     * @see Object#equals(Object)
     */
    // todo noetig oder reicht in super klasse?
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        // noinspection unchecked
        final D that = (D) o;

        if (Double.compare(that.affineDistance, affineDistance) != 0) return false;
        return Double.compare(that.subspaceDistance, subspaceDistance) == 0;
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int result;
        long temp;
        temp = subspaceDistance != +0.0d ? Double.doubleToLongBits(subspaceDistance) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = affineDistance != +0.0d ? Double.doubleToLongBits(affineDistance) : 0L;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Returns the value of the subspace distance.
     *
     * @return the value of the subspace distance
     */
    public double getSubspaceDistance() {
        return subspaceDistance;
    }

    /**
     * Returns the value of the affine distance.
     *
     * @return the value of the affine distance
     */
    public double getAffineDistance() {
        return affineDistance;
    }

    /**
     * Writes the subspace distance value and the affine distance value
     * of this SubspaceDistance to the specified stream.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(subspaceDistance);
        out.writeDouble(affineDistance);
    }

    /**
     * Reads the subspace distance value and the affine distance value
     * of this SubspaceDistance from the specified stream.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        subspaceDistance = in.readDouble();
        affineDistance = in.readDouble();
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     *
     * @return 16 (2 * 8 Byte for two double values)
     * @see Distance#externalizableSize()
     */
    public int externalizableSize() {
        return 16;
    }
}
