package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;

/**
 * Represents a unit in the CLIQUE algorithm.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf ModifiableDBIDs
 * 
 * @param <V> the type of NumberVector this unit contains
 */
public class CLIQUEUnit<V extends NumberVector<V, ?>> {
  /**
   * The one-dimensional intervals of which this unit is build.
   */
  private SortedSet<Interval> intervals;

  /**
   * Provides a mapping of particular dimensions to the intervals of which this
   * unit is build.
   */
  private Map<Integer, Interval> dimensionToInterval;

  /**
   * The ids of the feature vectors this unit contains.
   */
  private ModifiableDBIDs ids;

  /**
   * Flag that indicates if this unit is already assigned to a cluster.
   */
  private boolean assigned;

  /**
   * Creates a new k-dimensional unit for the given intervals.
   * 
   * @param intervals the intervals belonging to this unit
   * @param ids the ids of the feature vectors belonging to this unit
   */
  public CLIQUEUnit(SortedSet<Interval> intervals, ModifiableDBIDs ids) {
    this.intervals = intervals;

    dimensionToInterval = new HashMap<Integer, Interval>();
    for(Interval interval : intervals) {
      dimensionToInterval.put(interval.getDimension(), interval);
    }

    this.ids = ids;

    assigned = false;
  }

  /**
   * Creates a new one-dimensional unit for the given interval.
   * 
   * @param interval the interval belonging to this unit
   */
  public CLIQUEUnit(Interval interval) {
    intervals = new TreeSet<Interval>();
    intervals.add(interval);

    dimensionToInterval = new HashMap<Integer, Interval>();
    dimensionToInterval.put(interval.getDimension(), interval);

    ids = DBIDUtil.newHashSet();

    assigned = false;
  }

  /**
   * Returns true, if the intervals of this unit contain the specified feature
   * vector.
   * 
   * @param vector the feature vector to be tested for containment
   * @return true, if the intervals of this unit contain the specified feature
   *         vector, false otherwise
   */
  public boolean contains(V vector) {
    for(Interval interval : intervals) {
      double value = vector.doubleValue(interval.getDimension() + 1);
      if(interval.getMin() > value || value >= interval.getMax()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds the id of the specified feature vector to this unit, if this unit
   * contains the feature vector.
   * 
   * @param id Vector id
   * @param vector the feature vector to be added
   * @return true, if this unit contains the specified feature vector, false
   *         otherwise
   */
  public boolean addFeatureVector(DBID id, V vector) {
    if(contains(vector)) {
      ids.add(id);
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
   * Returns the selectivity of this unit, which is defined as the fraction of
   * total feature vectors contained in this unit.
   * 
   * @param total the total number of feature vectors
   * @return the selectivity of this unit
   */
  public double selectivity(double total) {
    return ids.size() / total;
  }

  /**
   * Returns a sorted set of the intervals of which this unit is build.
   * 
   * @return a sorted set of the intervals of which this unit is build
   */
  public SortedSet<Interval> getIntervals() {
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
   * Returns true if this unit contains the left neighbor of the specified
   * interval.
   * 
   * @param i the interval
   * @return true if this unit contains the left neighbor of the specified
   *         interval, false otherwise
   */
  public boolean containsLeftNeighbor(Interval i) {
    Interval interval = dimensionToInterval.get(i.getDimension());
    if(interval == null) {
      return false;
    }
    return interval.getMax() == i.getMin();
  }

  /**
   * Returns true if this unit contains the right neighbor of the specified
   * interval.
   * 
   * @param i the interval
   * @return true if this unit contains the right neighbor of the specified
   *         interval, false otherwise
   */
  public boolean containsRightNeighbor(Interval i) {
    Interval interval = dimensionToInterval.get(i.getDimension());
    if(interval == null) {
      return false;
    }
    return interval.getMin() == i.getMax();
  }

  /**
   * Returns true if this unit is already assigned to a cluster.
   * 
   * @return true if this unit is already assigned to a cluster, false
   *         otherwise.
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
   * 
   * @return the ids of the feature vectors this unit contains
   */
  public DBIDs getIds() {
    return ids;
  }

  /**
   * Joins this unit with the specified unit.
   * 
   * @param other the unit to be joined
   * @param all the overall number of feature vectors
   * @param tau the density threshold for the selectivity of a unit
   * @return the joined unit if the selectivity of the join result is equal or
   *         greater than tau, null otherwise
   */
  public CLIQUEUnit<V> join(CLIQUEUnit<V> other, double all, double tau) {
    Interval i1 = this.intervals.last();
    Interval i2 = other.intervals.last();
    if(i1.getDimension() >= i2.getDimension()) {
      return null;
    }

    Iterator<Interval> it1 = this.intervals.iterator();
    Iterator<Interval> it2 = other.intervals.iterator();
    SortedSet<Interval> resultIntervals = new TreeSet<Interval>();
    for(int i = 0; i < this.intervals.size() - 1; i++) {
      i1 = it1.next();
      i2 = it2.next();
      if(!i1.equals(i2)) {
        return null;
      }
      resultIntervals.add(i1);
    }
    resultIntervals.add(this.intervals.last());
    resultIntervals.add(other.intervals.last());

    HashSetModifiableDBIDs resultIDs = DBIDUtil.newHashSet(this.ids);
    resultIDs.retainAll(other.ids);

    if(resultIDs.size() / all >= tau) {
      return new CLIQUEUnit<V>(resultIntervals, resultIDs);
    }

    return null;
  }

  /**
   * Returns a string representation of this unit that contains the intervals of
   * this unit.
   * 
   * @return a string representation of this unit
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for(Interval interval : intervals) {
      result.append(interval).append(" ");
    }

    return result.toString();
  }
}