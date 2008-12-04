package experimentalcode.erich.visualization;

import org.w3c.dom.Element;

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
  public void useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    Element use = plot.svgElement(parent, "circle");
    SVGUtil.setAtt(use,"cx",x - size);
    SVGUtil.setAtt(use,"cy",y - size);
    SVGUtil.setAtt(use,"r",size);
    SVGUtil.setAtt(use,"style","fill:"+getColor(style));
  }

  /**
   * Colors enumeration.
   * 
   * @param style index
   * @return SVG color string
   */
  public static String getColor(int style) {
    String[] colors = { "red", "blue", "green", "orange", "cyan", "magenta", "yellow" };
    String colorstr = colors[style % colors.length];
    return colorstr;
  }
}
