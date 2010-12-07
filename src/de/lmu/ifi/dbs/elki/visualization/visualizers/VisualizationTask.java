package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.Result;
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
  
  /**
   * The active context
   */
  VisualizerContext<?> context;

  /**
   * The result we are attached to
   */
  Result result;

  /**
   * The current projection
   */
  Projection proj;

  /**
   * The plot to draw onto
   */
  SVGPlot svgp;

  /**
   * Width
   */
  double width;

  /**
   * Height
   */
  double height;

  /**
   * Constructor
   * 
   * @param context Context
   * @param result Result
   * @param proj Projection
   * @param svgp Plot
   * @param width Width
   * @param height Height
   */
  public VisualizationTask(VisualizerContext<?> context, Result result, Projection proj, SVGPlot svgp, double width, double height) {
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
  public <R extends Result> R getResult() {
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