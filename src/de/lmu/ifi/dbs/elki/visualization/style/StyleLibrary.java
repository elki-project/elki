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
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.VisualizationProjection#SCALE}.
   */
  public static final double SCALE = 1.0;

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