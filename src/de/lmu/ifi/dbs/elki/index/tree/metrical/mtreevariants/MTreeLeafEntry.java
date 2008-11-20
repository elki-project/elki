package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.AbstractEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of an M-Tree.
 * A MTreeLeafEntry consists of an id (representing the unique id of the underlying
 * object in the database) and the distance from the data object to its parent
 * routing object in the M-Tree.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used in the M-Tree
 */
public class MTreeLeafEntry<D extends Distance<D>> extends AbstractEntry implements MTreeEntry<D> {
    private static final long serialVersionUID = 1;

    /**
     * The distance from the underlying data object to its parent's routing object.
     */
    private D parentDistance;

    /**
     * Empty constructor for serialization purposes.
     */
    public MTreeLeafEntry() {
      // empty
    }

    /**
     * Provides a new MTreeLeafEntry object with the given parameters.
     *
     * @param objectID       the id of the underlying data object
     * @param parentDistance the distance from the underlying data object to its parent's routing object
     */
    public MTreeLeafEntry(Integer objectID, D parentDistance) {
        super(objectID);
        this.parentDistance = parentDistance;
    }

    /**
     * Returns the id of the underlying data object of this entry.
     *
     * @return the id of the underlying data object of this entry
     */
    public final Integer getRoutingObjectID() {
        return getID();
    }

    /**
     * Sets the id of the underlying data object of this entry.
     *
     * @param objectID the id to be set
     */
    public final void setRoutingObjectID(Integer objectID) {
        super.setID(objectID);
    }

    /**
     * Returns the distance from the underlying data object to its parent's routing object.
     *
     * @return the distance from the underlying data object to its parent's routing object
     */
    public final D getParentDistance() {
        return parentDistance;
    }

    /**
     * Sets the distance from the underlying data object to its parent's routing object.
     *
     * @param parentDistance the distance to be set
     */
    public final void setParentDistance(D parentDistance) {
        this.parentDistance = parentDistance;
    }

    /**
     * Returns null, since a leaf entry has no covering radius.
     *
     * @return null
     */
    public D getCoveringRadius() {
        return null;
    }

    /**
     * Throws an UnsupportedOperationException, since a leaf entry has no covering radius.
     *
     * @throws UnsupportedOperationException
     */
    public void setCoveringRadius(@SuppressWarnings("unused") D coveringRadius) {
        throw new UnsupportedOperationException("This entry is not a directory entry!");
    }

    /**
     * Returns true, since this entry is a leaf entry.
     *
     * @return true
     */
    public final boolean isLeafEntry() {
        return true;
    }

    /**
     * Calls the super method and writes the parentDistance of this entry to the specified
     * stream.
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(parentDistance);
    }

    /**
     * Calls the super method and reads the parentDistance of this entry from the specified
     * input stream.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.parentDistance = (D) in.readObject();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the object to be tested
     * @return true, if the super method returns true and
     *         o is an MTreeLeafEntry and has the same
     *         parentDistance as this entry.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final MTreeLeafEntry<D> that = (MTreeLeafEntry<D>) o;

        return !(parentDistance != null ?
            !parentDistance.equals(that.parentDistance) :
            that.parentDistance != null);
    }
}
