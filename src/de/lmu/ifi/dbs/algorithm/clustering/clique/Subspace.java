package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Interval;

import java.util.*;

/**
 * Represents a subspace of the original dataspace in the CLIQUE algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Subspace<V extends RealVector<V, ?>> implements Comparable<Subspace> {
  /**
   * The dense units belonging to this subspace.
   */
  private ArrayList<Unit<V>> denseUnits;

  /**
   * The dimensions building this subspace.
   */
  private SortedSet<Integer> dimensions;

  /**
   * The coverage of this subspace, which is the number of all feature vectors that fall inside
   * the dense units of this subspace.
   */
  private int coverage;

  /**
   * Creates a new one-dimensional subspace of the original dataspace.
   *
   * @param dimension the dimension building this subspace
   */
  Subspace(int dimension) {
    denseUnits = new ArrayList<Unit<V>>();
    coverage = 0;

    dimensions = new TreeSet<Integer>();
    dimensions.add(dimension);
  }

  /**
   * Adds the specified dense unit to this subspace.
   *
   * @param unit the unit to be added.
   */
  void addDenseUnit(Unit<V> unit) {
    Collection<Interval> intervals = unit.getIntervals();
    for (Interval interval : intervals) {
      if (!dimensions.contains(interval.getDimension())) {
        throw new IllegalArgumentException("Unit " + unit + "cannot be added to this subspace, " +
                                           "because of wrong dimensions!");
      }
    }

    denseUnits.add(unit);
    coverage += unit.numberOfFeatureVectors();
  }

  /**
   * Compares this subspace with the specified subspace for order.
   * Returns a negative integer, zero, or a positive integer if the coverage of this subspace
   * is less than, equal to, or greater than the coverage of the specified subspace.
   *
   * @param s the subspace to be compared
   * @return a negative integer, zero, or a positive integer if the coverage of this subspace
   *         is less than, equal to, or greater than the coverage of the specified subspace.
   */
  public int compareTo(Subspace s) {
    if (coverage == s.coverage) {
      if (dimensions.size() != s.dimensions.size()) {
        throw new IllegalArgumentException("different dimensions sizes!");
      }
      Iterator<Integer> it1 = dimensions.iterator();
      Iterator<Integer> it2 = s.dimensions.iterator();
      while (it1.hasNext()) {
        Integer d1 = it1.next();
        Integer d2 = it2.next();
        if (d1.equals(d2)) continue;
        return d1.compareTo(d2);
      }
    }

    if (coverage < s.coverage)
      return 1;

    return -1;
  }

  /**
   * Determines all clusters in this subspace by performing a
   * depth-first search algorithm to find connected dense units.
   *
   * @return the clusters in this subspace
   */
  List<Set<Integer>> determineClusters() {
    List<Set<Integer>> clusters = new ArrayList<Set<Integer>>();

    for (Unit<V> unit : denseUnits) {
      if (!unit.isAssigned()) {
        Set<Integer> cluster = new HashSet<Integer>();
        clusters.add(cluster);
        dfs(unit, cluster);
      }
    }

    return clusters;
  }

  /**
   * Depth-first search algorithm to find connected dense units in this subspace
   * that build a cluster. It starts with a unit, assigns it to a cluster and
   * finds all units it is connected to
   *
   * @param unit    the unit
   * @param cluster the ids of the feature vectors of the current cluster
   */
  void dfs(Unit<V> unit, Set<Integer> cluster) {
    cluster.addAll(unit.getIds());
    unit.markAsAssigned();

    for (Integer dim : dimensions) {
      Unit<V> left = leftNeighbour(unit, dim);
      if (left != null && !left.isAssigned())
        dfs(left, cluster);

      Unit<V> right = rightNeighbour(unit, dim);
      if (right != null && !right.isAssigned())
        dfs(right, cluster);
    }
  }

  /**
   * Returns the left neighbor of the given unit in the specified dimension.
   *
   * @param unit the unit to determine the left neighbor for
   * @param dim  the dimension
   * @return the left neighbor of the given unit in the specified dimension
   */
  Unit<V> leftNeighbour(Unit unit, Integer dim) {
    Interval i = unit.getInterval(dim);

    for (Unit<V> u : denseUnits) {
      if (u.containsLeftNeighbor(i))
        return u;
    }
    return null;
  }

  /**
   * Returns the right neighbor of the given unit in the specified dimension.
   *
   * @param unit the unit to determine the right neighbor for
   * @param dim  the dimension
   * @return the right neighbor of the given unit in the specified dimension
   */
  Unit<V> rightNeighbour(Unit unit, Integer dim) {
    Interval i = unit.getInterval(dim);

    for (Unit<V> u : denseUnits) {
      if (u.containsRightNeighbor(i))
        return u;
    }
    return null;
  }

  /**
   * Returns the coverage of this subspace, which is the number of
   * all feature vectors that fall inside
   * the dense units of this subspace.
   *
   * @return the coverage of this subspace
   */
  public int getCoverage() {
    return coverage;
  }

  /**
   * Returns the set of dimensions of this subspace.
   *
   * @return the dimensions of this subspace
   */
  public SortedSet<Integer> getDimensions() {
    return dimensions;
  }

  /**
   * Returns a string representation of this subspace
   * that contains the coverage, the dimensions and the
   * dense units of this subspace.
   *
   * @return a string representation of this subspace
   */
  public String toString() {
    return toString("");
  }

  /**
   * Returns a string representation of this subspace
   * that contains the coverage, the dimensions and the
   * dense units of this subspace.
   *
   * @param pre a string prefix
   * @return a string representation of this subspace
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("Coverage: " + coverage + "\n");
    result.append(pre).append("Dimensions: " + dimensions + "\n");
    result.append(pre).append("Units: ");
    for (Unit<V> denseUnit : denseUnits) {
      result.append(pre).append(denseUnit.toString() + "\n       ");
    }
    return result.toString();
  }
}
