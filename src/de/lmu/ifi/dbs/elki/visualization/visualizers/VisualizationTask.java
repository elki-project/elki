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
public class VisualizationTask extends AnyMap<String> implements Cloneable, Result {
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
   * Meta data key: Visualizer name for UI
   * 
   * Type: String
   */
  public final static String META_NAME = "name";

  /**
   * Meta data key: Level for visualizer ordering
   * 
   * Returns an integer indicating the "temporal position" of this Visualizer.
   * It is intended to impose an ordering on the execution of Visualizers as a
   * Visualizer may depend on another Visualizer running earlier. <br>
   * Lower numbers should result in a earlier use of this Visualizer, while
   * higher numbers should result in a later use. If more Visualizers have the
   * same level, no ordering is guaranteed. <br>
   * Note that this value is only a recommendation, as it is totally up to the
   * framework to ignore it.
   * 
   * Type: Integer
   */
  public final static String META_LEVEL = "level";

  /**
   * Flag to control visibility. Type: Boolean
   */
  public final static String META_VISIBLE = "visible";

  /**
   * Flag to signal there is no thumbnail needed. Type: Boolean
   */
  public final static String META_NOTHUMB = "no-thumbnail";

  /**
   * Mark as not having a (sensible) detail view.
   */
  public static final String META_NODETAIL = "no-detail";

  /**
   * Flag to signal the visualizer should not be exported. Type: Boolean
   */
  public final static String META_NOEXPORT = "no-export";

  /**
   * Flag to signal default visibility of a visualizer. Type: Boolean
   */
  public static final String META_VISIBLE_DEFAULT = "visible-default";

  /**
   * Flag to mark the visualizer as tool. Type: Boolean
   */
  public static final String META_TOOL = "tool";

  /**
   * Background layer
   */
  public final static int LEVEL_BACKGROUND = 0;

  /**
   * Data layer
   */
  public final static int LEVEL_DATA = 100;

  /**
   * Static plot layer
   */
  public final static int LEVEL_STATIC = 200;

  /**
   * Passive foreground layer
   */
  public final static int LEVEL_FOREGROUND = 300;

  /**
   * Active foreground layer (interactive elements)
   */
  public final static int LEVEL_INTERACTIVE = 1000;
  
  /**
   * Name
   */
  String name;

  /**
   * The active context
   */
  VisualizerContext<?> context;
  
  /**
   * The factory
   */
  VisFactory<?> factory;

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
   * Visualization task.
   * 
   * @param name Name
   * @param context Context
   * @param result Result
   * @param factory Factory
   */
  public VisualizationTask(String name, VisualizerContext<?> context, Result result, VisFactory<?> factory) {
    super();
    this.name = name;
    this.context = context;
    this.result = result;
    this.factory = factory;
  }

  /**
   * Constructor
   * 
   * @param name Name
   * @param context Context
   * @param result Result
   * @param proj Projection
   * @param svgp Plot
   * @param width Width
   * @param height Height
   */
  public VisualizationTask(String name, VisualizerContext<?> context, Result result, VisFactory<?> factory, Projection proj, SVGPlot svgp, double width, double height) {
    super();
    this.name = name;
    this.context = context;
    this.result = result;
    this.factory = factory;
    this.proj = proj;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
  }

  @SuppressWarnings("unchecked")
  public <O extends DatabaseObject> VisualizerContext<O> getContext() {
    return (VisualizerContext<O>) context;
  }

  /**
   * Get the visualizer factory.
   * 
   * @return Visualizer factory
   */
  public VisFactory<?> getFactory() {
    return factory;
  }

  @SuppressWarnings("unchecked")
  public <R extends Result> R getResult() {
    return (R) result;
  }

  @SuppressWarnings("unchecked")
  public <P extends Projection> P getProj() {
    return (P) proj;
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

  @Override
  public VisualizationTask clone() {
    VisualizationTask obj = (VisualizationTask) super.clone();
    obj.name = name;
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
  
  /**
   * Special clone operation to set projection and size.
   * 
   * @param plot new plot
   * @param p Projection to use
   * @param width Width
   * @param height Height
   * @return clone with different plot
   */
  public VisualizationTask clone(SVGPlot plot, Projection p, double width, double height) {
    VisualizationTask obj = this.clone();
    obj.svgp = plot;
    obj.proj = p;
    obj.width = width;
    obj.height = height;
    return obj;
  }

  @Override
  public String getLongName() {
    return name;
  }

  @Override
  public String getShortName() {
    return name;
  }
}