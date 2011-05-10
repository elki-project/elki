package de.lmu.ifi.dbs.elki.data.spatial;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.iterator.UnmodifiableIterator;

/**
 * Class representing a simple polygon.
 * 
 * @author Erich Schubert
 */
public class Polygon implements Iterable<double[]> {
  /**
   * The actual points
   */
  private Collection<double[]> points;

  /**
   * Constructor.
   * 
   * @param points Polygon points
   */
  public Polygon(Collection<double[]> points) {
    super();
    this.points = points;
  }

  @Override
  public Iterator<double[]> iterator() {
    return new UnmodifiableIterator<double[]>(points.iterator());
  }

  /**
   * Append the polygon to the buffer.
   * 
   * @param buf Buffer to append to
   */
  public void appendToBuffer(StringBuffer buf) {
    Iterator<double[]> iter = points.iterator();
    while(iter.hasNext()) {
      double[] data = iter.next();
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
}