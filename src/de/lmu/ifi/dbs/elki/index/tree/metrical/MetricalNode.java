package de.lmu.ifi.dbs.elki.index.tree.metrical;

import de.lmu.ifi.dbs.elki.index.tree.Node;

/**
 * Defines the requirements for an object that can be used as a node in a Metrical Index.
 * A metrical node can be a spatial metrical node or a metrical leaf node.
 *
 * @author Elke Achtert
 */
public interface MetricalNode<N extends MetricalNode<N,E>, E extends MetricalEntry> extends Node<N,E> {
	//TODO any methods?
}