package experimentalcode.erich.gearth;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Object representation consisting of (multiple) polygons.
 * 
 * @author Erich Schubert
 */
public class PolygonsObject {
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
}
