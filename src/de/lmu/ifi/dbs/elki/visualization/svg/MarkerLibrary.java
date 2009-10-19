package de.lmu.ifi.dbs.elki.visualization.svg;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;


/**
 * A marker library is a class that can generate and draw various styles of markers.
 * Different uses might require different marker libraries (e.g. full screen, thumbnail, print)
 * 
 * @author Erich Schubert
 *
 */
public interface MarkerLibrary {
  /**
   * Insert a marker at the given coordinates.
   * Markers will be defined in the defs part of the document, and then SVG-"use"d at the
   * given coordinates. This supposedly is more efficient and significantly reduces file size.
   * Symbols will be named "s0", "s1" etc.; these names must not be used by other elements in
   * the SVG document!
   * 
   * @param plot Plot to draw on
   * @param parent parent node
   * @param x coordinate
   * @param y coordinate
   * @param style style (enumerated)
   * @param size size
   * @return Element node generated.
   */
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size);
  
  /**
   * Set the color library for this marker library.
   * 
   * @param colors New color library to use.
   */
  public void setColorLibrary(ColorLibrary colors);
  
  /**
   * Get the color library of this marker library.
   * 
   * @return Color library in use.
   */
  public ColorLibrary getColorLibrary();
}