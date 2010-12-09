package de.lmu.ifi.dbs.elki.visualization.style.marker;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Simple marker library that just draws colored crosses at the given coordinates.
 * 
 * @author Erich Schubert
 *
 * @apiviz.composedOf ColorLibrary
 */
public class MinimalMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors;
  
  /**
   * Constructor
   * 
   * @param style Style library to use 
   */
  public MinimalMarkers(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }

  /**
   * Use a given marker on the document.
   */
  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int stylenr, double size) {
    Element marker = plot.svgRect(x - size / 2, y - size / 2, size, size);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY+":" + colors.getColor(stylenr));
    parent.appendChild(marker);
    return marker;
  }
}