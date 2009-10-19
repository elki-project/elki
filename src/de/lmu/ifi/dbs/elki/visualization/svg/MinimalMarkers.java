package de.lmu.ifi.dbs.elki.visualization.svg;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;

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
  public MinimalMarkers(ColorLibrary colors) {
    super();
    this.colors = colors;
  }

  /**
   * Constructor
   */
  public MinimalMarkers() {
    this(new PropertiesBasedColorLibrary());
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

  @Override
  public void setColorLibrary(ColorLibrary colors) {
    this.colors = colors;
  }

  @Override
  public ColorLibrary getColorLibrary() {
    return this.colors;
  }
}