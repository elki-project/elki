package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.util;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.Identifiable;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultIdentifiable;

/**
 * Encapsulates the attributes for a object that can be stored in a heap. The
 * object to be stored represents a node in a M-Tree and some additional
 * information. Additionally to the DefaultHeapNode this object holds the id of
 * the routing object of the underlying M-Tree node and its covering radius.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used in the M-Tree
 */
public class PQNode<D extends Distance<D>> extends DefaultHeapNode<D, Identifiable> {
    /**
     * Generated serialVersionUID.
     */
    private static final long serialVersionUID = -671978541634018004L;

    /**
     * The id of the routing object.
     */
    private Integer routingObjectID;

    /**
     * Empty constructor for serialization purposes.
     */
    public PQNode() {
        super();
    }

    /**
     * Creates a new heap node with the specified parameters.
     *
     * @param d_min           the minimum distance of the node
     * @param nodeID          the id of the node
     * @param routingObjectID the id of the routing object of the node
     */
    public PQNode(D d_min, final Integer nodeID, final Integer routingObjectID) {
        super(d_min, new DefaultIdentifiable(nodeID));
        this.routingObjectID = routingObjectID;
    }

    /**
     * Returns the id of the routing object.
     *
     * @return the id of the routing object
     */
    public Integer getRoutingObjectID() {
        return routingObjectID;
    }
}
