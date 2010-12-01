package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * A projected visualizer needs a projection for visualization.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface ProjectedVisFactory<O extends DatabaseObject> extends VisFactory<O> {
  // Empty. Only relevant for layouting
}