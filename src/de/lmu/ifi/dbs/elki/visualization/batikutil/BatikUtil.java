package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGLocatable;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGPoint;

/**
 * Batik helper class with static methods.
 * 
 * @author Erich Schubert
 */
public final class BatikUtil {
  /**
   * Get the relative coordinates of a point within the coordinate system of a
   * particular SVG Element.
   * 
   * @param evt Event, needs to be a DOMMouseEvent
   * @param reference SVG Element the coordinate system is used of
   * @return Array containing the X and Y values
   */
  public static double[] getRelativeCoordinates(Event evt, Element reference) {
    if(evt instanceof DOMMouseEvent && reference instanceof SVGLocatable && reference instanceof SVGElement) {
      // Get the screen (pixel!) coordinates
      DOMMouseEvent gnme = (DOMMouseEvent) evt;
      SVGMatrix mat = ((SVGLocatable) reference).getScreenCTM();
      SVGMatrix imat = mat.inverse();
      SVGPoint cPt = ((SVGElement) reference).getOwnerSVGElement().createSVGPoint();
      cPt.setX(gnme.getClientX());
      cPt.setY(gnme.getClientY());
      // Have Batik transform the screen (pixel!) coordinates into SVG element
      // coordinates
      cPt = cPt.matrixTransform(imat);

      return new double[] { cPt.getX(), cPt.getY() };
    }
    return null;
  }
}
