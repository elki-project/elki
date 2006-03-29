package de.lmu.ifi.dbs.index.spatial.rstar.deliclu;

import de.lmu.ifi.dbs.index.spatial.rstar.RTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * Default class for a node in a DeLiClu-Tree.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluNode extends RTreeNode
{
    /**
     * Indicates whether the entry has handled data objects or not.
     */
    private boolean hasHandled[];

    /**
     * Indicates whether the entry has unhandled data objects or not.
     */
    private boolean hasUnhandled[];

    /**
     * Empty constructor for Externalizable interface.
     */
    public DeLiCluNode()
    {
    }

    /**
     * Creates a new Node object.
     * 
     * @param file
     *            the file storing the RTree
     * @param capacity
     *            the capacity (maximum number of entries plus 1 for overflow)
     *            of this node
     * @param isLeaf
     *            indicates wether this node is a leaf node
     */
    public DeLiCluNode(PageFile<RTreeNode> file, int capacity, boolean isLeaf)
    {
        super(file, capacity, isLeaf);
        this.hasHandled = new boolean[capacity];
        this.hasUnhandled = new boolean[capacity];
        Arrays.fill(hasUnhandled, true);
    }

    /**
     * Creates a new leaf node with the specified capacity.
     * 
     * @param capacity
     *            the capacity of the new node
     * @return a new leaf node
     */
    protected RTreeNode createNewLeafNode(int capacity)
    {
        return new DeLiCluNode(file, capacity, true);
    }

    /**
     * Creates a new directory node with the specified capacity.
     * 
     * @param capacity
     *            the capacity of the new node
     * @return a new directory node
     */
    protected RTreeNode createNewDirectoryNode(int capacity)
    {
        return new DeLiCluNode(file, capacity, false);
    }

    /**
     * Returns true, if the entry at the specified index has handled objects,
     * false otherwise.
     * 
     * @param i
     *            the index of the entry
     * @return true, if the entry at the specified index has handled objects,
     *         false otherwise.
     */
    public final boolean hasHandled(int i)
    {
        return hasHandled[i];
    }

    /**
     * Returns true, if the entry at the specified index has unhandled objects,
     * false otherwise.
     * 
     * @param i
     *            the index of the entry
     * @return true, if the entry at the specified index has unhandled objects,
     *         false otherwise.
     */
    public final boolean hasUnhandled(int i)
    {
        return hasUnhandled[i];
    }

    /**
     * Marks the entry at the specified to have handled data objects. Returns
     * true, if all entries in this node have handled data objects, false
     * othwerwise.
     * 
     * @param i
     *            the index of the entry to be marked to have handled data
     *            objects
     * @return true, if all entries in this node have handled data objects,
     *         false othwerwise
     */
    public boolean setHasHandled(int i)
    {
        hasHandled[i] = true;

        for (boolean h : hasHandled)
        {
            if (!h)
                return false;
        }
        return true;
    }

    /**
     * Marks the entry at the specified to have no unhandled data objects.
     * 
     * @param i
     *            the index of the entry to be marked to have no unhandled data
     *            objects
     */
    public void resetHasUnhandled(int i)
    {
        hasUnhandled[i] = false;
    }

    /**
     * The object implements the writeExternal method to save its contents by
     * calling the methods of DataOutput for its primitive values or calling the
     * writeObject method of ObjectOutput for objects, strings, and arrays.
     * 
     * @param out
     *            the stream to write the object to
     * @throws java.io.IOException
     *             Includes any I/O exceptions that may occur
     * @serialData Overriding methods should use this tag to describe the data
     *             layout of this Externalizable object. List the sequence of
     *             element types and, if possible, relate the element to a
     *             public/protected field and/or method of this Externalizable
     *             class.
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeObject(hasHandled);
        out.writeObject(hasUnhandled);
    }

    /**
     * The object implements the readExternal method to restore its contents by
     * calling the methods of DataInput for primitive types and readObject for
     * objects, strings and arrays. The readExternal method must read the values
     * in the same sequence and with the same types as were written by
     * writeExternal.
     * 
     * @param in
     *            the stream to read data from in order to restore the object
     * @throws java.io.IOException
     *             if I/O errors occur
     * @throws ClassNotFoundException
     *             If the class for an object being restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException
    {
        super.readExternal(in);
        this.hasHandled = (boolean[]) in.readObject();
        this.hasUnhandled = (boolean[]) in.readObject();
    }

}
