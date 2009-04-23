package experimentalcode.erich.visualization.svg;

import org.apache.batik.util.SVGConstants;

/**
 * Simple color library with a small number of hand-picked colors that provide sufficient
 * difference when printed.
 * 
 * @author Erich Schubert
 */
public class PublicationColorLibrary implements ColorLibrary {
  private static String[] colors = { SVGConstants.CSS_RED_VALUE, SVGConstants.CSS_BLUE_VALUE, SVGConstants.CSS_GREEN_VALUE, SVGConstants.CSS_ORANGE_VALUE, SVGConstants.CSS_CYAN_VALUE, SVGConstants.CSS_MAGENTA_VALUE, SVGConstants.CSS_YELLOW_VALUE };

  @Override
  public String getColor(int index) {
    return colors[index % colors.length];
  }

  @Override
  public int getNumberOfNativeColors() {
    return colors.length;
  }
}
