package de.lmu.ifi.dbs.elki.visualization.colors;

import org.apache.batik.util.SVGConstants;

/**
 * Simple color library using a few grey values for printing.
 * 
 * @author Erich Schubert
 */
public class GreyscaleColorLibrary implements ColorLibrary {
  /**
   * A few gray values.
   */
  private static String[] colors = {
    // black
    SVGConstants.CSS_BLACK_VALUE,
    // gray - 80 = 50% white
    SVGConstants.CSS_GRAY_VALUE,
    // silver - C0 = 75% white
    SVGConstants.CSS_SILVER_VALUE,
    // dim gray - 69 = 41% white
    SVGConstants.CSS_DIMGRAY_VALUE,
    // dark gray - A9 = 66% white
    SVGConstants.CSS_DARKGRAY_VALUE,
    /* TOO BRIGHT: */
    // light gray - D3 = 82% white
    /* SVGConstants.CSS_LIGHTGRAY_VALUE, */
    // white - FF = 100% white
    /* SVGConstants.CSS_WHITE_VALUE */
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
