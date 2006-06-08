package de.lmu.ifi.dbs.index.metrical.mtree.util;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeEntry;

/**
 * Helper class: encapsulates an entry in a M-Tree and a distance value
 * belonging to this entry. This class is used for splitting nodes.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DistanceEntry<D extends Distance<D>> implements
        Comparable<DistanceEntry<D>>
{
    /**
     * The entry of the M-Tree.
     */
    private MTreeEntry<D> entry;

    /**
     * The distance value belonging to the entry.
     */
    private D distance;

    /**
     * The index of this entry in its parent.
     */
    private Integer index;

    /**
     * Constructs a new DistanceEntry object with the specified parameters.
     * 
     * @param entry
     *            the entry of the M-Tree
     * @param distance
     *            the distance value belonging to the entry
     * @param index
     *            the index of this entry in its parent
     */
    public DistanceEntry(MTreeEntry<D> entry, D distance, Integer index)
    {
        this.entry = entry;
        this.distance = distance;
        this.index = index;
    }

    /**
     * Returns the entry of the M-Tree.
     * 
     * @return the entry of the M-Tree
     */
    public MTreeEntry<D> getEntry()
    {
        return entry;
    }

    /**
     * Returns the distance value belonging to the entry.
     * 
     * @return the distance value belonging to the entry
     */
    public D getDistance()
    {
        return distance;
    }

    /**
     * Returns the index of this entry in its parent.
     * 
     * @return the index of this entry in its parent
     */
    public Integer getIndex()
    {
        return index;
    }

    /**
     * Compares this object with the specified object for order.
     * 
     * @param o
     *            the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @throws ClassCastException
     *             if the specified object's type prevents it from being
     *             compared to this Object.
     */
    public int compareTo(DistanceEntry<D> o)
    {
        int comp = distance.compareTo(o.distance);
        if (comp != 0)
            return comp;

        if (entry.isLeafEntry() || o.entry.isLeafEntry())
            return entry.getRoutingObjectID().compareTo(o.entry.getRoutingObjectID());

        MTreeDirectoryEntry<D> dirEntry = (MTreeDirectoryEntry<D>) entry;
        MTreeDirectoryEntry<D> otherDirEntry = (MTreeDirectoryEntry<D>) o.entry;
        comp = dirEntry.getNodeID().compareTo(otherDirEntry.getRoutingObjectID());
        if (comp != 0)
            return comp;
        return entry.getRoutingObjectID().compareTo(o.entry.getRoutingObjectID());
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        if (entry.isLeafEntry())
            return "" + entry.getRoutingObjectID() + "(" + distance + ")";
        return "" + ((MTreeDirectoryEntry<D>) entry).getNodeID() + "("
                + distance + ")";
    }
}
