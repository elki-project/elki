package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.data.RealVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a unit in the CLIQUE algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUEUnit<V extends RealVector<V, ?>> {
  /**
   * The one dimensional intervals of which this unit is build.
   */
  private Collection<CLIQUEInterval> intervals;

  /**
   * The ids of the feature vectors this unit contains.
   */
  private Set<Integer> ids;

  /**
   * Creates a new 1 dimensional unit for the given hyper point.
   *
   * @param interval the interval belonging to this unit
   */
  public CLIQUEUnit(CLIQUEInterval interval) {
    intervals = new ArrayList<CLIQUEInterval>();
    intervals.add(interval);

    ids = new HashSet<Integer>();
  }

  /**
   * Retuns true, if the intervals of this unit contain the specified
   * feature vector.
   *
   * @param vector the feature vector to be tested for containment
   * @return true, if the intervals of this unit contain the specified
   *         feature vector, false otherwise
   */
  public boolean contains(V vector) {
    for (CLIQUEInterval interval : intervals) {
      double value = vector.getValue(interval.getDimension() + 1).doubleValue();
      if (interval.getMin() > value || value >= interval.getMax())
        return false;
    }
    return true;
  }

  /**
   * Adds the id of the specified feature vector to this unit, if
   * this unit contains the feature vector.
   *
   * @param vector the feature vector to be added
   * @return true, if this unit contains the specified
   *         feature vector, false otherwise
   */
  public boolean addFeatureVector(V vector) {
    if (contains(vector)) {
      ids.add(vector.getID());
      return true;
    }
    return false;
  }

  /**
   * Returns the number of feature vectors this unit contains.
   *
   * @return the number of feature vectors this unit contains
   */
  public int numberOfFeatureVectors() {
    return ids.size();
  }

  /**
   * Returns the selectivity of this unit, which is defined
   * as the fraction of total feature vectors contained in this unit.
   *
   * @param total the total number of feature vectors
   * @return the selectivity of this unit
   */
  public double selectivity(double total) {
    return ((double) ids.size()) / total;
  }


  /**
   * Returns a collection of the intervals of which this unit is build.
   *
   * @return a collection of the intervals of which this unit is build
   */
  public Collection<CLIQUEInterval> getIntervals() {
    return intervals;
  }
}
