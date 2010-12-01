package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;

/**
 * An unprojected Visualizer can run stand-alone.
 * 
 * @author Erich Schubert
 */
public interface UnprojectedVisFactory<O extends DatabaseObject> extends VisFactory<O> {
  // Empty. Only relevant for layouting
}