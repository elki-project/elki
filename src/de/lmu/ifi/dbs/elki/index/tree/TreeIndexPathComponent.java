package de.lmu.ifi.dbs.elki.index.tree;

/**
 * Represents a component in an IndexPath. A component in an IndexPath consists
 * of the entry of the index (representing a node or a data object) and the index
 * of the component in its parent.
 *
 * @author Elke Achtert
 * @param <E> the type of Entry used in the index
 */
public class TreeIndexPathComponent<E extends Entry> {
    /**
     * The entry of this component.
     */
    private E entry;

    /**
     * The index of this component in its parent.
     */
    private Integer index;

    /**
     * Creates a new IndexPathComponent.
     *
     * @param entry the entry of the component
     * @param index index of the component in its parent
     */
    public TreeIndexPathComponent(E entry, Integer index) {
        this.entry = entry;
        this.index = index;
    }

    /**
     * Returns the entry of the component.
     *
     * @return the entry of the component
     */
    public E getEntry() {
        return entry;
    }

    /**
     * Returns the index of the component in its parent.
     *
     * @return the index of the component in its parent
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * Returns <code>true</code> if <code>this == o</code>
     * has the value <code>true</code> or
     * o is not null and
     * o is of the same class as this instance
     * and if the entry of this component equals
     * the entry of the o argument,
     * <code>false</code> otherwise.
     *
     * @see Entry#equals(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TreeIndexPathComponent<E> that = (TreeIndexPathComponent<E>) o;
        return (this.entry.equals(that.entry));
    }

    /**
     * Returns a hash code for this component. The hash code of a TreeIndexPathComponent
     * is defined to be the hash code of its entry.
     *
     * @return the hash code of the entry of this component
     */
    @Override
    public int hashCode() {
        return entry.hashCode();
    }

    /**
     * Returns a string representation of this component.
     *
     * @return a string representation of the entry of this component
     *         followd by the index of this component in its parent
     */
    @Override
    public String toString() {
        return entry.toString() + " [" + index + "]";
    }
}
