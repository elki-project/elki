package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;

/**
 * Unprojected visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UnprojectedThumbnail
 */
public abstract class AbstractUnprojectedVisFactory<O extends DatabaseObject> extends AbstractVisFactory<O> implements UnprojectedVisFactory<O> {
  /**
   * Constructor
   * 
   * @param name A short name characterizing the visualizer
   * @param level Level
   */
  protected AbstractUnprojectedVisFactory(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor with name
   * 
   * @param name A short name characterizing the visualizer
   */
  protected AbstractUnprojectedVisFactory(String name) {
    super(name);
  }
}