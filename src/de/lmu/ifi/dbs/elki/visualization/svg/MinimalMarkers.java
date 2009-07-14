package de.lmu.ifi.dbs.elki.visualization.svg;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PublicationColorLibrary;


/**
 * Simple marker library that just draws colored crosses at the given coordinates.
 * 
 * @author Erich Schubert
 *
 */
public class MinimalMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors = new PublicationColorLibrary();
  
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
    Element marker = plot.svgRect(x - size / 2, y - size / 2, size, size);
    SVGUtil.setStyle(marker, "fill:" + colors.getColor(style));
    parent.appendChild(marker);
    return marker;
  }
}
