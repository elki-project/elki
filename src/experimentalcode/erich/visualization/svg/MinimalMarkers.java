package experimentalcode.erich.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import experimentalcode.erich.visualization.SVGPlot;

/**
 * Simple marker library that just draws colored crosses at the given coordinates.
 * 
 * @author Erich Schubert
 *
 */
public class MinimalMarkers implements MarkerLibrary {
  /**
   * Constructor
   */
  public MinimalMarkers() {
    super();
  }

  /**
   * Use a given marker on the document.
   */
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    Element marker = plot.svgElement(SVGConstants.SVG_RECT_TAG);
    SVGUtil.setAtt(marker, SVGConstants.SVG_X_ATTRIBUTE, x - size / 2);
    SVGUtil.setAtt(marker, SVGConstants.SVG_Y_ATTRIBUTE, y - size / 2);
    SVGUtil.setAtt(marker, SVGConstants.SVG_WIDTH_ATTRIBUTE, size);
    SVGUtil.setAtt(marker, SVGConstants.SVG_HEIGHT_ATTRIBUTE, size);
    SVGUtil.setAtt(marker, SVGConstants.SVG_STYLE_ATTRIBUTE, "fill:" + getColor(style));
    parent.appendChild(marker);
    return marker;
  }

  /**
   * Colors enumeration.
   * 
   * @param style index
   * @return SVG color string
   */
  public static String getColor(int style) {
    String[] colors = { SVGConstants.CSS_RED_VALUE, SVGConstants.CSS_BLUE_VALUE, SVGConstants.CSS_GREEN_VALUE, SVGConstants.CSS_ORANGE_VALUE, SVGConstants.CSS_CYAN_VALUE, SVGConstants.CSS_MAGENTA_VALUE, SVGConstants.CSS_YELLOW_VALUE };
    String colorstr = colors[style % colors.length];
    return colorstr;
  }
}
