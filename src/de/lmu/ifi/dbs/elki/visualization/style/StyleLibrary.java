package de.lmu.ifi.dbs.elki.visualization.style;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;

/**
 * Style library interface. A style library allows the user to customize the
 * visual rendering, for example for print media or screen presentation without
 * having to change program code.
 * 
 * @author Erich Schubert
 */
public interface StyleLibrary {
  /**
   * Default
   */
  final static String DEFAULT = "";

  /**
   * Page
   */
  final static String PAGE = "page";

  /**
   * Plot
   */
  final static String PLOT = "plot";

  /**
   * Axis
   */
  final static String AXIS = "axis";

  /**
   * Axis tick
   */
  final static String AXIS_TICK = "axis.tick";

  /**
   * Axis minor tick
   */
  final static String AXIS_TICK_MINOR = "axis.tick.minor";

  /**
   * Axis label
   */
  final static String AXIS_LABEL = "axis.label";

  /**
   * Key
   */
  final static String KEY = "key";

  /**
   * Clusterorder
   */
  final static String CLUSTERORDER = "plot.clusterorder";
  
  /**
   * Margin
   */
  final static String MARGIN = "margin";
  
  /**
   * Bubble size
   */
  final static String BUBBLEPLOT = "plot.bubble";
  
  /**
   * Marker size
   */
  final static String MARKERPLOT = "plot.marker";

  /**
   * Dot size
   */
  final static String DOTPLOT = "plot.dot";

  /**
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.VisualizationProjection#SCALE}.
   */
  public static final double SCALE = 1.0;

  /*   ** Property types ** */
  /**
   * Color
   */
  final static String COLOR = "color";

  /**
   * Background color
   */
  final static String BACKGROUND_COLOR = "background-color";

  /**
   * Text color
   */
  final static String TEXT_COLOR = "text-color";

  /**
   * Color set
   */
  final static String COLORSET = "colorset";

  /**
   * Line width
   */
  final static String LINE_WIDTH = "line-width";

  /**
   * Text size
   */
  final static String TEXT_SIZE = "text-size";

  /**
   * Font family
   */
  final static String FONT_FAMILY = "font-family";

  /**
   * Generic size
   */
  final static String GENERIC_SIZE = "size";

  /**
   * Retrieve a color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  public String getColor(String name);

  /**
   * Retrieve background color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  public String getBackgroundColor(String name);

  /**
   * Retrieve text color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  public String getTextColor(String name);

  /**
   * Retrieve colorset for an item
   * 
   * @param name Reference name
   * @return color library
   */
  public ColorLibrary getColorSet(String name);

  /**
   * Get line width
   * 
   * @param key Key
   * @return line width as double
   */
  public double getLineWidth(String key);

  /**
   * Get generic size
   * 
   * @param key Key
   * @return size as double
   */
  public double getSize(String key);

  /**
   * Get text size
   * 
   * @param key Key
   * @return line width as double
   */
  public double getTextSize(String key);

  /**
   * Get font family
   * 
   * @param key Key
   * @return font family CSS string
   */
  public String getFontFamily(String key);
}