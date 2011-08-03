package de.lmu.ifi.dbs.elki.data.spatial;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Object representation consisting of (multiple) polygons.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Polygon
 */
public class PolygonsObject implements SpatialComparable {
  /**
   * Static (empty) prototype
   */
  public static final PolygonsObject PROTOTYPE = new PolygonsObject(null);

  /**
   * The polygons
   */
  private Collection<Polygon> polygons;

  /**
   * Constructor.
   * 
   * @param polygons Polygons
   */
  public PolygonsObject(Collection<Polygon> polygons) {
    super();
    this.polygons = polygons;
    if(this.polygons == null) {
      this.polygons = Collections.emptyList();
    }
  }

  /**
   * Access the polygon data.
   * 
   * @return Polygon collection
   */
  public Collection<Polygon> getPolygons() {
    return Collections.unmodifiableCollection(polygons);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    appendToBuffer(buf);
    return buf.toString();
  }

  /**
   * Append polygons to the buffer.
   * 
   * @param buf Buffer to append to
   */
  public void appendToBuffer(StringBuffer buf) {
    Iterator<Polygon> iter = polygons.iterator();
    while(iter.hasNext()) {
      Polygon poly = iter.next();
      poly.appendToBuffer(buf);
      if(iter.hasNext()) {
        buf.append(" -- ");
      }
    }
  }

  @Override
  public int getDimensionality() {
    assert (polygons.size() > 0);
    return polygons.iterator().next().getDimensionality();
  }

  @Override
  public double getMin(int dimension) {
    double min = Double.MAX_VALUE;
    for(Polygon p : polygons) {
      min = Math.min(min, p.getMin(dimension));
    }
    return min;
  }

  @Override
  public double getMax(int dimension) {
    double max = Double.MIN_VALUE;
    for(Polygon p : polygons) {
      max = Math.max(max, p.getMin(dimension));
    }
    return max;
  }
}