package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.data.RealVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a subspace of the original dataspace in the CLIQUE algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUESubspace<V extends RealVector<V, ?>> implements Comparable<CLIQUESubspace<V>> {
  /**
   * The dense units belonging to this subspace.
   */
  private Collection<CLIQUEUnit<V>> denseUnits;

  /**
   * The dimensions building this subspace.
   */
  private Set<Integer> dimensions;

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
  public CLIQUESubspace(int dimension) {
    denseUnits = new ArrayList<CLIQUEUnit<V>>();
    coverage = 0;

    dimensions = new HashSet<Integer>();
    dimensions.add(dimension);
  }

  /**
   * Adds the specified dense unit to this subspace.
   *
   * @param unit the unit to be added.
   */
  public void addDenseUnit(CLIQUEUnit<V> unit) {
    Collection<CLIQUEInterval> intervals = unit.getIntervals();
    for (CLIQUEInterval interval : intervals) {
      if (!dimensions.contains(interval.getDimension())) {
        throw new IllegalArgumentException("Unit " + unit + "cannot be added to this subspace, " +
                                           "because of wrong dimensions!");
      }
    }

    denseUnits.add(unit);
    coverage += unit.numberOfFeatureVectors();
  }


  /**
   * Compares this object with the specified object for order.
   * Returns a negative integer, zero, or a positive integer if the coverage of this subspace
   * is less than, equal to, or greater than the coverage of the specified subspace.
   *
   * @param s the subspace to be compared
   * @return a negative integer, zero, or a positive integer if the coverage of this subspace
   *         is less than, equal to, or greater than the coverage of the specified subspace.
   */
  public int compareTo(CLIQUESubspace<V> s) {
    //if (coverage == s.coverage)
      //return dimensions.compareTo(s.dimensions);
    //todo

    if (coverage < s.coverage)
      return 1;

    return -1;
  }
}
