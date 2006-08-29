package de.lmu.ifi.dbs.index.metrical;

import de.lmu.ifi.dbs.index.Node;

/**
 * Defines the requirements for an object that can be used as a node in a Metrical Index.
 * A metrical node can be a spatial metrical node or a metrical leaf node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface MetricalNode<N extends MetricalNode<N,E>, E extends MetricalEntry> extends Node<N,E> {
}
