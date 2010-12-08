package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisFactory;

/**
 * Unprojected visualizer.
 * 
 * @author Erich Schubert
 */
public abstract class UnpVisFactory<O extends DatabaseObject> extends AbstractVisFactory<O> implements UnprojectedVisFactory<O> {
  /**
   * Constructor with name
   */
  protected UnpVisFactory() {
    super();
  }
}