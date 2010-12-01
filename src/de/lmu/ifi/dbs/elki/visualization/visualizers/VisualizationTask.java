package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Container class, with ugly casts to reduce generics crazyness.
 * 
 * @author Erich Schubert
 */
public class VisualizationTask extends AnyMap<String> implements Cloneable {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constant for using thumbnail
   */
  public static final String THUMBNAIL = "thumbnail";

  /**
   * Thumbnail size
   */
  public static final String THUMBNAIL_RESOLUTION = "tres";
  
  VisualizerContext<?> context;

  AnyResult result;

  Projection proj;

  SVGPlot svgp;

  double width;

  double height;

  /**
   * @param context
   * @param result
   * @param proj
   * @param svgp
   * @param width
   * @param height
   */
  public VisualizationTask(VisualizerContext<?> context, AnyResult result, Projection proj, SVGPlot svgp, double width, double height) {
    super();
    this.context = context;
    this.result = result;
    this.proj = proj;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
  }

  public SVGPlot getPlot() {
    return svgp;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  @SuppressWarnings("unchecked")
  public <O extends DatabaseObject> VisualizerContext<O> getContext() {
    return (VisualizerContext<O>) context;
  }

  @SuppressWarnings("unchecked")
  public <R extends AnyResult> R getResult() {
    return (R) result;
  }

  @SuppressWarnings("unchecked")
  public <P extends Projection> P getProj() {
    return (P) proj;
  }

  @Override
  public VisualizationTask clone() {
    VisualizationTask obj = (VisualizationTask) super.clone();
    obj.context = context;
    obj.result = result;
    obj.proj = proj;
    obj.svgp = svgp;
    obj.width = width;
    obj.height = height;
    return obj;
  }
  
  /**
   * Special clone operation that replaces the target plot.
   * 
   * @param newplot Replacement plot to use
   * @return clone with different plot
   */
  public VisualizationTask clone(SVGPlot newplot) {
    VisualizationTask obj = this.clone();
    obj.svgp = newplot;
    return obj;
  }
}