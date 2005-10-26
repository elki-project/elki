package de.lmu.ifi.dbs.index.metrical;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.Index;

/**
 * Defines the requirements for a metrical index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface MetricalIndex<O extends MetricalObject, D extends Distance> extends Index<O> {
}
