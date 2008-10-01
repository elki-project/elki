package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a LOF-Table.
 *
 * @author Elke Achtert
 */
public class LOFEntry implements Externalizable {
    private static final long serialVersionUID = 1;

    /**
     * The sum of the reachability distances between o and its neighbors.
     */
    private double sum1;

    /**
     * For each neighbor p of o: The sum of the reachability distances
     * between p and its neighbors.
     */
    private double[] sum2Array;

    /**
     * Empty constructor for serialization purposes.
     */
    public LOFEntry() {
        // empty constructor
    }

    /**
     * Creates a new entry in a lof table.
     *
     * @param sum1      the sum of the reachability distances between o and its neighbors
     * @param sum2Array for each neighbor p of o: the sum of the reachability distances
     *                  between p and its neighbors
     */
    public LOFEntry(double sum1, double[] sum2Array) {
        this.sum1 = sum1;
        this.sum2Array = sum2Array;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(sum1);
        for (double sum2 : this.sum2Array) {
            result.append(" ");
            result.append(sum2);
        }
        return result.toString();
    }

    /**
     * Returns the sum of the reachability distances between o and its neighbors.
     *
     * @return the sum of the reachability distances between o and its neighbors
     */
    public double getSum1() {
        return sum1;
    }

    /**
     * Sets the sum of the reachability distances between o and its neighbors.
     *
     * @param sum1 the value to be set
     */
    public void setSum1(double sum1) {
        this.sum1 = sum1;
    }

    /**
     * Returns the ith sum2, which is for the ith neighbor p of o
     * the sum of the reachability distances between p and its neighbors
     *
     * @param i the index of the neighbor p
     * @return the ith sum2
     */
    public double getSum2(int i) {
        return sum2Array[i];
    }

    /**
     * Returns the sum2 array, which is for each neighbor p of o
     * the sum of the reachability distances between p and its neighbors.
     *
     * @return sum2 array
     */
    public double[] getSum2Array() {
        return sum2Array;
    }

    /**
     * Sets the ith sum2 value
     *
     * @param i    the index in the sum2Array
     * @param sum2 the value to be set
     */
    public void setSum2(int i, double sum2) {
        sum2Array[i] = sum2;
    }

    /**
     * Returns the local outlier factor
     *
     * @return the local outlier factor
     */
    public double getLOF() {
        double sum_2 = 0.0;
        for (double s2 : sum2Array) {
            sum_2 += 1 / s2;
        }

        return 1 / ((double) sum2Array.length) * sum1 * sum_2;
    }

    /**
     * Inserts the given sum2 value at the specified index in the sum2Array.
     * All elements starting at index are shifted one position right,
     * the (former) last element will be removed.
     *
     * @param index the index in the sum2Array to insert the value in
     * @param sum2  the value to be inserted
     */
    public void insertAndMoveSum2(int index, double sum2) {
        for (int i = sum2Array.length - 1; i > index; i--) {
            sum2Array[i] = sum2Array[i - 1];
        }
        sum2Array[index] = sum2;
    }

    /**
     * The object implements the writeExternal method to save its contents
     * by calling the methods of DataOutput for its primitive values or
     * calling the writeObject method of ObjectOutput for objects, strings,
     * and arrays.
     *
     * @param out the stream to write the object to
     * @throws java.io.IOException Includes any I/O exceptions that may occur
     * @serialData Overriding methods should use this tag to describe
     * the data layout of this Externalizable object.
     * List the sequence of element types and, if possible,
     * relate the element to a public/protected field and/or
     * method of this Externalizable class.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(sum1);
        out.writeInt(sum2Array.length);
        for (double sum2 : sum2Array)
            out.writeDouble(sum2);
    }

    /**
     * The object implements the readExternal method to restore its
     * contents by calling the methods of DataInput for primitive
     * types and readObject for objects, strings and arrays.  The
     * readExternal method must read the values in the same sequence
     * and with the same types as were written by writeExternal.
     *
     * @param in the stream to read data from in order to restore the object
     * @throws java.io.IOException    if I/O errors occur
     * @throws ClassNotFoundException If the class for an object being
     *                                restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sum1 = in.readDouble();
        int m = in.readInt();
        sum2Array = new double[m];
        for (int i = 0; i < m; i++) {
            sum2Array[i] = in.readDouble();
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the o argument,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final LOFEntry lofEntry = (LOFEntry) o;

        if (Math.abs(sum1 - lofEntry.sum1) > 0.000000000001) {
            return false;
        }

        for (int i = 0; i < sum2Array.length; i++) {
            if (Math.abs(sum2Array[i] - lofEntry.sum2Array[i]) > 0.000000000001) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        final long temp = sum1 != 0.0d ? Double.doubleToLongBits(sum1) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }
}
