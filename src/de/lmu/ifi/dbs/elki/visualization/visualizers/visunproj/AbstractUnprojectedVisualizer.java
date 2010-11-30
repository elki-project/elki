package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.UnprojectedThumbnail;

/**
 * Unprojected visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses UnprojectedThumbnail
 */
public abstract class AbstractUnprojectedVisualizer<O extends DatabaseObject> extends AbstractVisualizer<O> implements UnprojectedVisualizer<O> {
  /**
   * Constructor
   * 
   * @param name A short name characterizing the visualizer
   * @param level Level
   */
  protected AbstractUnprojectedVisualizer(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor with name
   * 
   * @param name A short name characterizing the visualizer
   */
  protected AbstractUnprojectedVisualizer(String name) {
    super(name);
  }
  
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, double width, double height, int tresolution) {
    return new UnprojectedThumbnail<O>(this, context, svgp, width, height, tresolution, 0);
  }
}