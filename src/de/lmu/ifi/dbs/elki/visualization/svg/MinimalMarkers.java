package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

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
  private ColorLibrary colors;
  
  /**
   * Constructor
   */
  public MinimalMarkers(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }

  /**
   * Constructor
   */
  public MinimalMarkers() {
    this(new PropertiesBasedStyleLibrary());
  }

  /**
   * Use a given marker on the document.
   */
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int stylenr, double size) {
    Element marker = plot.svgRect(x - size / 2, y - size / 2, size, size);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY+":" + colors.getColor(stylenr));
    parent.appendChild(marker);
    return marker;
  }

  @Override
  public void setStyleLibrary(StyleLibrary style) {
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }
}