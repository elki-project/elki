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
public class Polygon implements Iterable<Vector>, SpatialComparable {
  /**
   * The actual points
   */
  private List<Vector> points;

  /**
   * Minimum values
   */
  private double[] min = null;

  /**
   * Maximum values
   */
  private double[] max = null;

  /**
   * Constructor.
   * 
   * @param points Polygon points
   */
  public Polygon(List<Vector> points) {
    super();
    this.points = points;
    // Compute the bounds.
    if(points.size() > 0) {
      final Iterator<Vector> iter = points.iterator();
      final Vector first = iter.next();
      final int dim = first.getDimensionality();
      min = first.getArrayCopy();
      max = first.getArrayCopy();
      while(iter.hasNext()) {
        Vector next = iter.next();
        for(int i = 0; i < dim; i++) {
          final double cur = next.get(i);
          min[i] = Math.min(min[i], cur);
          max[i] = Math.max(max[i], cur);
        }
      }
    }
  }

  public Polygon(List<Vector> points, double minx, double maxx, double miny, double maxy) {
    super();
    this.points = points;
    this.min = new double[] { minx, miny };
    this.max = new double[] { maxx, maxy };
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

  @Override
  public int getDimensionality() {
    return min.length;
  }

  @Override
  public double getMin(int dimension) {
    return min[dimension - 1];
  }

  @Override
  public double getMax(int dimension) {
    return max[dimension - 1];
  }
}