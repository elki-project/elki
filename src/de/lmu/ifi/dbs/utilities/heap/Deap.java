package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.persistent.Page;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Identifiable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Vector;

/**
 * Subclass of a MinMaxHeap that can be an entry in a persistent heap.
 * 
 * @author Elke Achtert 
 */
class Deap<K extends Comparable<K> & Serializable, V extends Identifiable & Serializable>
        extends MinMaxHeap<K, V> implements Page<Deap<K,V>>
{

    /**
     * The index of this Deap in the persistent heap.
     */
    private int index;

    /**
     * The index of this deap in the cachePath of the persistent heap.
     */
    private int cacheIndex;

    /**
     * The maximum size of this deap.
     */
    private int maxSize;

    /**
     * The dirty flag of this page.
     */
    private boolean dirty;

    /**
     * Empty constructor for serialization purposes.
     */
    public Deap()
    {
        super();
    }

    /**
     * Creates a new Deap with the specified parameters.
     * 
     * @param maxSize
     *            the maximum size of the deap
     * @param index
     *            the index of this deap in the persistent heap
     * @param cacheIndex
     *            the index of this deap in the cachePath of the persistent heap
     */
    public Deap(final int maxSize, int index, int cacheIndex)
    {
        super();
        this.index = index;
        this.cacheIndex = cacheIndex;
        this.maxSize = maxSize;
    }

    /**
     * Returns the index of this deap in the cachePath of the persistent heap.
     * 
     * @return the index of this deap in the cachePath of the persistent heap
     */
    public int getCacheIndex()
    {
        return cacheIndex;
    }

    /**
     * Sets the index of this deap in the cachePath of the persistent heap.
     * 
     * @param cacheIndex
     *            the cache index to be set
     */
    public void setCacheIndex(int cacheIndex)
    {
        this.cacheIndex = cacheIndex;
    }

    /**
     * Returns true if this deap is full, false otherwise.
     * 
     * @return true if this deap is full, false otherwise
     */
    public boolean isFull()
    {
        return this.size() == maxSize;
    }

    /**
     * Moves all elements from this deap into the specified deap.
     * 
     * @param other
     *            the deap to move all elements to
     */
    public void moveAll(Deap<K, V> other)
    {
        other.heap = this.heap;
        other.indices = this.indices;

        this.heap = new Vector<HeapNode<K, V>>();
    }

    /**
     * Returns the unique id of this Page.
     * 
     * @return the unique id of this Page
     */
    public Integer getID()
    {
        return index;
    }

    /**
     * Sets the unique id of this Page.
     * 
     * @param id
     *            the id to be set
     */
    public void setID(int id)
    {
        throw new UnsupportedOperationException("Should never happen!");
    }

    /**
     * Sets the page file of this page.
     * 
     * @param file
     *            the page file to be set
     */
    public void setFile(PageFile file)
    {
    	// TODO do nothing?
    }

    /**
     * Returns true if this page is dirty, false otherwise.
     * 
     * @return true if this page is dirty, false otherwise
     */
    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * Sets the dirty flag of this page.
     * 
     * @param dirty
     *            the dirty flag to be set
     */
    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
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
        out.writeObject(heap);
        out.writeInt(maxSize);
        out.writeInt(index);
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
        this.heap = (Vector<HeapNode<K, V>>) in.readObject();
        this.maxSize = in.readInt();
        this.index = in.readInt();
        this.cacheIndex = -1;
        this.dirty = false;
    }

    /**
     * Returns the index of this deap in the persistent heap.
     * 
     * @return the index of this deap in the persistent heap
     */
    public int getIndex()
    {
        return index;
    }
}
