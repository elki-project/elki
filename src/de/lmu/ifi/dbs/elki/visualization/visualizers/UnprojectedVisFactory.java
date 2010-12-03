package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * An unprojected Visualizer can run stand-alone.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface UnprojectedVisFactory<O extends DatabaseObject> extends VisFactory<O> {
  // Empty. Only relevant for layouting
}