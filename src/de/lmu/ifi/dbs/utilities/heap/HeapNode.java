package de.lmu.ifi.dbs.utilities.heap;

import de.lmu.ifi.dbs.utilities.Identifiable;

import java.io.Serializable;

/**
 * Defines the requirements for an object that can be used as a node in a Heap.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface HeapNode<K extends Comparable<K>, V extends Identifiable>
        extends Comparable<HeapNode<K, V>>, Serializable
{

    /**
     * Returns the key of this HeapNode.
     * 
     * @return the key of this HeapNode
     */
    K getKey();

    /**
     * Sets the key of this HeapNode.
     * 
     * @param key
     *            the key to be set
     */
    void setKey(K key);

    /**
     * Returns the value of this HeapNode.
     * 
     * @return the value of this HeapNode
     */
    V getValue();

    /**
     * Sets the value of this HeapNode.
     * 
     * @param value
     *            the value to be set
     */
    void setValue(V value);
}
