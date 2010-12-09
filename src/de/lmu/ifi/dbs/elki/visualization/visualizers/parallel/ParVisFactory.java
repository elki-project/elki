package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;

/**
 * Visualizer using parallel coordinates.
 * 
 * @author Erich Schubert
 */
public abstract class ParVisFactory<O extends DatabaseObject> extends AbstractVisFactory<O> {
  /**
   * Constructor with name
   */
  protected ParVisFactory() {
    super();
  }
}