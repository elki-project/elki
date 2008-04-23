package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.Interval;

import java.util.*;

/**
 * Represents a unit in the CLIQUE algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Unit<V extends RealVector<V, ?>> {
  /**
   * The one-dimensional intervals of which this unit is build.
   */
  private Collection<Interval> intervals;

  /**
   * Provides a mapping of particular dimensions to the intervals of which this unit is build.
   */
  private Map<Integer, Interval> dimensionToInterval;

  /**
   * The ids of the feature vectors this unit contains.
   */
  private Set<Integer> ids;

  /**
   * Flag that indicates if this unit is already assigned to a cluster.
   */
  private boolean assigned;

  /**
   * Creates a new one-dimensional unit for the given interval.
   *
   * @param interval the interval belonging to this unit
   */
  public Unit(Interval interval) {
    intervals = new ArrayList<Interval>();
    intervals.add(interval);

    dimensionToInterval = new HashMap<Integer, Interval>();
    dimensionToInterval.put(interval.getDimension(), interval);

    ids = new HashSet<Integer>();

    assigned = false;
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
    for (Interval interval : intervals) {
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
  public Collection<Interval> getIntervals() {
    return intervals;
  }

  /**
   * Returns the interval of the specified dimension.
   *
   * @param dimension the dimension of the interval to be returned
   * @return the interval of the specified dimension
   */
  public Interval getInterval(Integer dimension) {
    return dimensionToInterval.get(dimension);
  }

  /**
   * Returns true if this unit contains the
   * left neighbor of the specified interval.
   *
   * @param i the interval
   * @return true if this unit contains the
   *         left neighbor of the specified interval, false otherwise
   */
  public boolean containsLeftNeighbor(Interval i) {
    Interval interval = dimensionToInterval.get(i.getDimension());
    if (interval == null) return false;
    return interval.getMax() == i.getMin();
  }

  /**
   * Returns true if this unit contains the
   * right neighbor of the specified interval.
   *
   * @param i the interval
   * @return true if this unit contains the
   *         right neighbor of the specified interval, false otherwise
   */
  public boolean containsRightNeighbor(Interval i) {
    Interval interval = dimensionToInterval.get(i.getDimension());
    if (interval == null) return false;
    return interval.getMin() == i.getMax();
  }

  /**
   * Returns true if this unit is already assigned to a cluster.
   *
   * @return true if this unit is already assigned to a cluster, false otherwise.
   */
  public boolean isAssigned() {
    return assigned;
  }

  /**
   * Marks this unit as assigned to a cluster.
   */
  public void markAsAssigned() {
    this.assigned = true;
  }

  /**
   * Returns the ids of the feature vectors this unit contains.
   * @return the ids of the feature vectors this unit contains
   */
  public Set<Integer> getIds() {
    return ids;
  }

  /**
   * Returns a string representation of this unit
   * that contains the intervals of this unit.
   *
   * @return a string representation of this unit
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (Interval interval : intervals)
      result.append(interval);

    return result.toString();
  }
}
