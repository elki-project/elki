package experimentalcode.erich.visualization.svg;

import org.w3c.dom.Element;

import experimentalcode.erich.visualization.SVGPlot;

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
    Element use = plot.svgElement(parent, "rect");
    SVGUtil.setAtt(use,"x",x - size/2);
    SVGUtil.setAtt(use,"y",y - size/2);
    SVGUtil.setAtt(use,"width",size);
    SVGUtil.setAtt(use,"height",size);
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
