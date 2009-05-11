package de.lmu.ifi.dbs.elki.visualization.colors;

import org.apache.batik.util.SVGConstants;

/**
 * Simple color library with a small number of hand-picked colors that provide sufficient
 * difference when printed (and that should work with all printers).
 * 
 * @author Erich Schubert
 */
public class PublicationColorLibrary implements ColorLibrary {
  /**
   * These colors are hand-picked to provide reasonable contrast
   * and readability. Therefore we stick to primary colors first,
   * and the first two colors are red and blue to help red-green blind people.
   * Yellow usually offers bad contrast, therefore comes late.
   * Magenta often shows up too similar to red, cyan too similar to blue.
   */
  private static String[] colors = {
    // red
    SVGConstants.CSS_RED_VALUE,
    // blue
    SVGConstants.CSS_BLUE_VALUE,
    // green
    SVGConstants.CSS_GREEN_VALUE,
    // orange
    SVGConstants.CSS_ORANGE_VALUE,
    // cyan
    SVGConstants.CSS_CYAN_VALUE,
    // magenta
    SVGConstants.CSS_MAGENTA_VALUE,
    // yellow
    SVGConstants.CSS_YELLOW_VALUE
  };

  @Override
  public String getColor(int index) {
    return colors[index % colors.length];
  }

  @Override
  public int getNumberOfNativeColors() {
    return colors.length;
  }
}
