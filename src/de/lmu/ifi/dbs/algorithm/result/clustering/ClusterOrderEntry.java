package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Identifiable;

/**
 * Provides an entry in a cluster order.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 * @param <D> the type of Distance used by the ClusterOrderEntry
 */
public class ClusterOrderEntry<D extends Distance<D>> implements Identifiable<ClusterOrderEntry<D>> {
    /**
     * The id of the entry.
     */
    private Integer objectID;

    /**
     * The id of the entry's predecessor.
     */
    private Integer predecessorID;

    /**
     * The reachability of the entry.
     */
    private D reachability;

    /**
     * Creates a new entry in a cluster order with the specified parameters.
     *
     * @param objectID      the id of the entry
     * @param predecessorID the id of the entry's predecessor
     * @param reachability  the reachability of the entry
     */
    public ClusterOrderEntry(Integer objectID, Integer predecessorID, D reachability) {
        this.objectID = objectID;
        this.predecessorID = predecessorID;
        this.reachability = reachability;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object has the same attribute
     *         values as the o argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        // noinspection unchecked
        final ClusterOrderEntry<D> that = (ClusterOrderEntry<D>) o;

        if (!objectID.equals(that.objectID)) return false;
        if (predecessorID != null ? !predecessorID.equals(that.predecessorID) : that.predecessorID != null)
            return false;
        return reachability.equals(that.reachability);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return the object id if this entry
     */
    @Override
    public int hashCode() {
        return objectID.hashCode();
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return objectID + "(" + predecessorID + "," + reachability + ")";
    }

    /**
     * Returns the object id of this entry.
     *
     * @return the object id of this entry
     */
    public Integer getID() {
        return objectID;
    }

    /**
     * Returns the id of the predecessor of this entry if this entry has a predecessor, null otherwise.
     *
     * @return the id of the predecessor of this entry
     */
    public Integer getPredecessorID() {
        return predecessorID;
    }

    /**
     * Returns the reachability distance of this entry
     *
     * @return the reachability distance of this entry
     */
    public D getReachability() {
        return reachability;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(Identifiable<ClusterOrderEntry<D>> o) {
        return this.objectID - o.getID();
    }

}
