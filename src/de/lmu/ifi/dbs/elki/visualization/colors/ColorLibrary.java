package de.lmu.ifi.dbs.elki.visualization.colors;

/**
 * Color scheme interface
 * 
 * @author Erich Schubert
 *
 */
public interface ColorLibrary {
  /**
   * List of line colors
   */
  final static String COLOR_LINE_COLORS = "line.colors";
  /**
   * Named color for the page background
   */
  final static String COLOR_PAGE_BACKGROUND = "page.background";
  /**
   * Named color for a typical axis
   */
  final static String COLOR_AXIS_LINE = "axis.line";
  /**
   * Named color for a typical axis tick mark
   */
  final static String COLOR_AXIS_TICK = "axis.tick";
  /**
   * Named color for a typical axis tick mark
   */
  final static String COLOR_AXIS_MINOR_TICK = "axis.tick.minor";
  /**
   * Named color for a typical axis label
   */
  final static String COLOR_AXIS_LABEL = "axis.label";
  /**
   * Named color for the background of the key box
   */
  final static String COLOR_KEY_BACKGROUND = "key.background";
  /**
   * Named color for a label in the key part
   */
  final static String COLOR_KEY_LABEL = "key.label";
  /**
   * Return the number of native colors available. These are guaranteed to be unique.
   * 
   * @return number of native colors
   */
  public int getNumberOfNativeColors();
  /**
   * Return the i'th color.
   * 
   * @param index color index
   * @return color in hexadecimal notation (#aabbcc) or color name ("red") as valid in CSS and SVG.
   */
  public String getColor(int index);
  /**
   * Retrieve named colors (e.g. "background")
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such as "red"
   */
  public String getNamedColor(String name);
}