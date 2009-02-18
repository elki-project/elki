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
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    Element marker = plot.svgElement("rect");
    SVGUtil.setAtt(marker,"x",x - size/2);
    SVGUtil.setAtt(marker,"y",y - size/2);
    SVGUtil.setAtt(marker,"width",size);
    SVGUtil.setAtt(marker,"height",size);
    SVGUtil.setAtt(marker,"style","fill:"+getColor(style));
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
    String[] colors = { "red", "blue", "green", "orange", "cyan", "magenta", "yellow" };
    String colorstr = colors[style % colors.length];
    return colorstr;
  }
}
