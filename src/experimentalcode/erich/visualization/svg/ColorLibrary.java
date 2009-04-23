package experimentalcode.erich.visualization.svg;

/**
 * Color scheme interface
 * 
 * @author Erich Schubert
 *
 */
public interface ColorLibrary {
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
}