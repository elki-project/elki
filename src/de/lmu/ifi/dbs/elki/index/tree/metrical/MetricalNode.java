package de.lmu.ifi.dbs.elki.index.tree.metrical;

import de.lmu.ifi.dbs.elki.index.tree.Node;

/**
 * Defines the requirements for an object that can be used as a node in a Metrical Index.
 * A metrical node can be a metrical directory node or a metrical leaf node.
 *
 * @author Elke Achtert
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 * @see de.lmu.ifi.dbs.elki.index.tree.Node
 */
public interface MetricalNode<N extends MetricalNode<N, E>, E extends MetricalEntry> extends Node<N, E> {
}