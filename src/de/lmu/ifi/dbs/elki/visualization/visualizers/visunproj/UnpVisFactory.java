package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;

/**
 * Unprojected visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.landmark
 */
public abstract class UnpVisFactory<O extends DatabaseObject> extends AbstractVisFactory<O> {
  /**
   * Constructor with name
   */
  protected UnpVisFactory() {
    super();
  }
}