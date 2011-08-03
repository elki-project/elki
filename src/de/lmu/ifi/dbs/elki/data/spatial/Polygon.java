package de.lmu.ifi.dbs.elki.data.spatial;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.iterator.ReverseListIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.UnmodifiableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.UnmodifiableListIterator;

/**
 * Class representing a simple polygon. While you can obviously store non-simple
 * polygons in this, note that many of the supplied methods will assume that the
 * polygons are simple.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Vector
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
   * Get a list iterator.
   * 
   * @return List iterator.
   */
  public ListIterator<Vector> listIterator() {
    return new UnmodifiableListIterator<Vector>(points.listIterator());
  }

  /**
   * Return an iterator that iterates the list backwards.
   * 
   * @return Reversed iterator
   */
  public ListIterator<Vector> descendingIterator() {
    return new UnmodifiableListIterator<Vector>(new ReverseListIterator<Vector>(points));
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

  /**
   * Get a vector by index.
   * 
   * @param idx Index to get
   * @return Vector
   */
  public Vector get(int idx) {
    return points.get(idx);
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

  /**
   * Test polygon orientation.
   * 
   * @return -1, 0, 1 for counterclockwise, undefined and clockwise.
   */
  public int testClockwise() {
    if(points.size() < 3) {
      return 0;
    }
    final int size = points.size();
    // Count the number of positive and negative oriented angles
    int c = 0;

    // TODO: faster when using an iterator?
    for(int i = 0; i < size; i++) {
      // Three consecutive points
      final int j = (i + 1) % size;
      final int k = (i + 2) % size;
      final double dxji = points.get(j).get(0) - points.get(i).get(0);
      final double dykj = points.get(k).get(1) - points.get(j).get(1);
      final double dyji = points.get(j).get(1) - points.get(i).get(1);
      final double dxkj = points.get(k).get(0) - points.get(j).get(0);
      final double z = (dxji * dykj) - (dyji * dxkj);
      if(z < 0) {
        c--;
      }
      else if(z > 0) {
        c++;
      }
    }
    if(c > 0) {
      return -1;
    }
    else if(c < 0) {
      return +1;
    }
    else {
      return 0;
    }
  }
}