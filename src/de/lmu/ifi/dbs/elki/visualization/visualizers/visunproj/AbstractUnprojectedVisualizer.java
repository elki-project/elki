package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Unprojected visualizer.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractUnprojectedVisualizer<O extends DatabaseObject> extends AbstractVisualizer<O> implements UnprojectedVisualizer<O> {
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, double width, double height, int tresolution) {
    return new UnprojectedThumbnail<O>(this, context, svgp, width, height, tresolution);
  }
}