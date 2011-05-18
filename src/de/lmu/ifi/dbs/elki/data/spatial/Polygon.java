package de.lmu.ifi.dbs.elki.data.spatial;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.iterator.UnmodifiableIterator;

/**
 * Class representing a simple polygon.
 * 
 * @author Erich Schubert
 */
public class Polygon implements Iterable<Vector> {
  /**
   * The actual points
   */
  private List<Vector> points;

  /**
   * Constructor.
   * 
   * @param points Polygon points
   */
  public Polygon(List<Vector> points) {
    super();
    this.points = points;
  }

  @Override
  public Iterator<Vector> iterator() {
    return new UnmodifiableIterator<Vector>(points.iterator());
  }

  /**
   * Append the polygon to the buffer.
   * 
   * @param buf Buffer to append to
   */
  public void appendToBuffer(StringBuffer buf) {
    Iterator<Vector> iter = points.iterator();
    while(iter.hasNext()) {
      double[] data = iter.next().getArrayRef();
      for(int i = 0; i < data.length; i++) {
        if(i > 0) {
          buf.append(",");
        }
        buf.append(data[i]);
      }
      if(iter.hasNext()) {
        buf.append(" ");
      }
    }
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    appendToBuffer(buf);
    return buf.toString();
  }

  /**
   * Get the polygon length.
   * 
   * @return Polygon length
   */
  public int size() {
    return points.size();
  }
}