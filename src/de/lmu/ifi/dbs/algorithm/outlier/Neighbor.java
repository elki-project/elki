package de.lmu.ifi.dbs.algorithm.outlier;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a NNTable, encapsulates information about neighboring objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Neighbor implements Externalizable, Comparable<Neighbor> {
    /**
     * The object id;
     */
    private Integer objectID;

    /**
     * The id of the neighboring object.
     */
    private Integer neighborID;

    /**
     * The index of the neighboring object
     * in the object's kNN array.
     */
    private int index;

    /**
     * The distance between the object and its neighbor.
     */
    private double distance;

    /**
     * The reachability distance of the neighbor w.r.t. the object:
     * reachDist(object, neighbor) = max(kNNDist(neighbor), dist(object, neighbor))
     */
    private double reachabilityDistance;

    /**
     * Empty constructor for serialization purposes.
     */
    public Neighbor() {
        // empty constructor
    }

    /**
     * Provides a new neighbor object with the specified parameters.
     *
     * @param objectID             the object id
     * @param index                the index of the neighboring object in the object's kNN array
     * @param neighborID           the id of the neighboring object
     * @param reachabilityDistance the reachability distance of the neighbor w.r.t. the object
     * @param distance             the distance between the object and its neighbor
     */
    public Neighbor(Integer objectID, int index, Integer neighborID,
                    double reachabilityDistance, double distance) {
        this.objectID = objectID;
        this.index = index;
        this.neighborID = neighborID;
        this.reachabilityDistance = reachabilityDistance;
        this.distance = distance;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        return "(" + objectID + ", " + index + ", " + neighborID +
               ", " + reachabilityDistance + ", " + distance + ")";

    }

    /**
     * Returns the object id.
     *
     * @return the object id
     */
    public Integer getObjectID() {
        return objectID;
    }

    /**
     * Returns the id of the neighboring object.
     *
     * @return the id of the neighboring object
     */
    public Integer getNeighborID() {
        return neighborID;
    }

    /**
     * Returns the index of the neighboring object in the object's kNN array.
     *
     * @return the index of the neighboring object in the object's kNN array
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the reachability distance of the neighbor w.r.t. the object.
     *
     * @return the reachability distance of the neighbor w.r.t. the object.
     */
    public double getReachabilityDistance() {
        return reachabilityDistance;
    }

    /**
     * Returns the distance between the object and its neighbor.
     *
     * @return the distance between the object and its neighbor
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the o argument,
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Neighbor neighbor = (Neighbor) o;

        if (Double.compare(neighbor.distance, distance) != 0) return false;
        if (index != neighbor.index) return false;
        if (Double.compare(neighbor.reachabilityDistance, reachabilityDistance) != 0) return false;
        return neighborID.equals(neighbor.neighborID);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int result;
        long temp;
        result = neighborID.hashCode();
        result = 29 * result + index;
        temp = distance != +0.0d ? Double.doubleToLongBits(distance) : 0L;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        temp = reachabilityDistance != +0.0d ? Double.doubleToLongBits(reachabilityDistance) : 0L;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Creates and returns a copy of this object.
     */
    public Neighbor copy() {
        return new Neighbor(objectID,
                            index,
                            neighborID,
                            reachabilityDistance,
                            distance);
    }

    /**
     * The object implements the writeExternal method to save its contents
     * by calling the methods of DataOutput for its primitive values or
     * calling the writeObject method of ObjectOutput for objects, strings,
     * and arrays.
     *
     * @param out the stream to write the object to
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(objectID);
        out.writeInt(neighborID);
        out.writeInt(index);
        out.writeDouble(distance);
        out.writeDouble(reachabilityDistance);
    }

    /**
     * The object implements the readExternal method to restore its
     * contents by calling the methods of DataInput for primitive
     * types and readObject for objects, strings and arrays.  The
     * readExternal method must read the values in the same sequence
     * and with the same types as were written by writeExternal.
     *
     * @param in the stream to read data from in order to restore the object
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        objectID = in.readInt();
        neighborID = in.readInt();
        index = in.readInt();
        distance = in.readDouble();
        reachabilityDistance = in.readDouble();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param other the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(Neighbor other) {
        int comp = this.objectID - other.objectID;
        if (comp != 0) return comp;

        comp = this.index - other.index;
        if (comp != 0) return comp;

        comp = this.neighborID - other.neighborID;
        if (comp != 0) return comp;

        if (this.distance < other.distance) return -1;
        if (this.distance > other.distance) return 1;
        if (this.reachabilityDistance < other.reachabilityDistance) return -1;
        if (this.reachabilityDistance > other.reachabilityDistance) return 1;

        return 0;
    }
}
