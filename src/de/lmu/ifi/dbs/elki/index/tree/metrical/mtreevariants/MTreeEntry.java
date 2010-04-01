package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalEntry;

/**
 * Defines the requirements for an entry in an M-Tree node.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used in the M-Tree
 */

public interface MTreeEntry<D extends Distance<D>> extends MetricalEntry {
    /**
     * Returns the id of the underlying database object of this entry, if this entry is a
     * leaf entry, the id of the routing object, otherwise.
     *
     * @return the id of the underlying database object of this entry, if this entry is a
     *         leaf entry, the id of the routing object, otherwise
     */
    Integer getRoutingObjectID();

    /**
     * Sets the id of the underlying database object of this entry, if this entry is a leaf entry,
     * the id of the routing object, otherwise.
     *
     * @param objectID the id to be set
     */
    void setRoutingObjectID(Integer objectID);

    /**
     * Returns the distance from the routing object of this entry to
     * the routing object of its parent.
     *
     * @return the distance from the object to its parent object
     */
    D getParentDistance();

    /**
     * Sets the distance from the routing object to
     * routing object of its parent.
     *
     * @param parentDistance the distance to be set
     */
    void setParentDistance(D parentDistance);

    /**
     * Returns the covering radius if this entry is a directory entry,
     * null otherwise.
     *
     * @return the covering radius of this entry
     */
    D getCoveringRadius();

    /**
     * Sets the covering radius of this entry if this entry is a directory entry,
     * throws an UnsupportedOperationException otherwise.
     *
     * @param coveringRadius the covering radius to be set
     */
    void setCoveringRadius(D coveringRadius);
}
