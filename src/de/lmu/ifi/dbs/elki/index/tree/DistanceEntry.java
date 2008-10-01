package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * Helper class: encapsulates an entry in an Index and a distance value
 * belonging to this entry.
 *
 * @author Elke Achtert
 * @param <E> the type of Entry used in the index
 * @param <D> the type of Distance used
 */
public class DistanceEntry<D extends Distance<D>, E extends Entry> implements Comparable<DistanceEntry<D, E>> {
    /**
     * The entry of the Index.
     */
    private E entry;

    /**
     * The distance value belonging to the entry.
     */
    private D distance;

    /**
     * The index of the entry in its parent's child array.
     */
    private int index;

    /**
     * Constructs a new DistanceEntry object with the specified parameters.
     *
     * @param entry    the entry of the Index
     * @param distance the distance value belonging to the entry
     * @param index    the index of the entry in its parent' child array
     */
    public DistanceEntry(E entry, D distance, int index) {
        this.entry = entry;
        this.distance = distance;
        this.index = index;
    }

    /**
     * Returns the entry of the Index.
     *
     * @return the entry of the Index
     */
    public E getEntry() {
        return entry;
    }

    /**
     * Returns the distance value belonging to the entry.
     *
     * @return the distance value belonging to the entry
     */
    public D getDistance() {
        return distance;
    }

    /**
     * Returns the index of this entry in its parents child array.
     *
     * @return the index of this entry in its parents child array
     */
    public int getIndex() {
        return index;
    }


    /**
     * Compares this object with the specified object for order.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it from being
     *                            compared to this Object.
     */
    public int compareTo(DistanceEntry<D, E> o) {
        int comp = distance.compareTo(o.distance);
        if (comp != 0)
            return comp;

        return entry.getID().compareTo(o.entry.getID());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "" + entry.getID() + "(" + distance + ")";
    }
}
