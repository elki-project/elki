package de.lmu.ifi.dbs.elki.visualization.style;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;

/**
 * Style library interface
 * 
 * @author Erich Schubert
 *
 */
public interface StyleLibrary {
  /* ** TREE ** */
  
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
   * Retrieve a color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such as "red"
   */
  public String getColor(String name);
  
  /**
   * Retrieve background color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such as "red"
   */
  public String getBackgroundColor(String name);
  
  /**
   * Retrieve text color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such as "red"
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