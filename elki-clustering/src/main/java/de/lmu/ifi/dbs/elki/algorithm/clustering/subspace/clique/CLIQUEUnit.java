/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Represents a unit in the CLIQUE algorithm.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - CLIQUEInterval
 * @composed - - - ModifiableDBIDs
 */
public class CLIQUEUnit {
  /**
   * The dimensions involved in this subspace.
   */
  private int[] dims;

  /**
   * The bounding values (min, max) for each dimension.
   */
  private double[] bounds;

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
   * @param prefix Prefix unit that will be extended by one dimension
   * @param newdim Additional dimension
   * @param min Minimum bound
   * @param max Maximum bound
   * @param ids the ids of the feature vectors belonging to this unit
   */
  private CLIQUEUnit(CLIQUEUnit prefix, int newdim, double min, double max, ModifiableDBIDs ids) {
    int dimensionality = prefix.dims.length + 1;
    this.dims = Arrays.copyOf(prefix.dims, dimensionality);
    dims[dimensionality - 1] = newdim;
    this.bounds = Arrays.copyOf(prefix.bounds, dimensionality << 1);
    bounds[(dimensionality - 1) << 1] = min;
    bounds[(dimensionality << 1) - 1] = max;
    this.ids = ids;
    assigned = false;
  }

  /**
   * Creates a new one-dimensional unit for the given interval.
   * 
   * @param dim Dimension
   * @param min Minimum
   * @param max MAximum
   */
  public CLIQUEUnit(int dim, double min, double max) {
    dims = new int[] { dim };
    bounds = new double[] { min, max };
    ids = DBIDUtil.newHashSet();
    assigned = false;
  }

  /**
   * Get the dimensionality of this unit.
   *
   * @return Number of dimensions constrained.
   */
  public int dimensionality() {
    return dims.length;
  }

  /**
   * Get the ith dimension constrained.
   *
   * @param i Index
   * @return dimension
   */
  public int getDimension(int i) {
    return dims[i];
  }

  /**
   * Returns true, if the intervals of this unit contain the specified feature
   * vector.
   * 
   * @param vector the feature vector to be tested for containment
   * @return true, if the intervals of this unit contain the specified feature
   *         vector, false otherwise
   */
  public boolean contains(NumberVector vector) {
    for(int i = 0; i < dims.length; i++) {
      final double value = vector.doubleValue(dims[i]);
      if(bounds[i << 1] > value || value >= bounds[(i << 1) + 1]) {
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
  public boolean addFeatureVector(DBIDRef id, NumberVector vector) {
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
   * Returns true if this unit is the left neighbor of the given unit.
   * 
   * @param unit Reference unit
   * @param d Current dimension
   * @return true if this unit is the left neighbor of the given unit
   */
  protected boolean containsLeftNeighbor(CLIQUEUnit unit, int d) {
    final int e = dims.length - 1;
    return checkDimensions(unit, e) && bounds[(e << 1) + 1] == unit.bounds[e << 1];
  }

  /**
   * Returns true if this unit is the right neighbor of the given unit.
   * 
   * @param unit Reference unit
   * @param d Current dimension
   * @return true if this unit is the right neighbor of the given unit
   */
  protected boolean containsRightNeighbor(CLIQUEUnit unit, int d) {
    final int e = dims.length - 1;
    return checkDimensions(unit, e) && bounds[e << 1] == unit.bounds[(e << 1) + 1];
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
  protected CLIQUEUnit join(CLIQUEUnit other, double all, double tau) {
    if(other.dimensionality() != this.dimensionality()) {
      return null;
    }
    // n-1 dimensions must be the same:
    int e = dims.length - 1;
    if(!checkDimensions(other, e)) {
      return null;
    }
    if(dims[e] >= other.dims[e]) {
      return null;
    }

    HashSetModifiableDBIDs resultIDs = DBIDUtil.newHashSet(this.ids);
    resultIDs.retainAll(other.ids);

    if(resultIDs.size() / all < tau) {
      return null;
    }
    return new CLIQUEUnit(this, other.dims[e], other.bounds[e << 1], other.bounds[(e << 1) + 1], resultIDs);
  }

  /**
   * Check that the first e dimensions agree.
   *
   * @param other Other unit
   * @param e Number of dimensions to check
   * @return {@code true} if the first e dimensions are the same (index and
   *         bounds)
   */
  private boolean checkDimensions(CLIQUEUnit other, int e) {
    for(int i = 0, j = 0; i < e; i++, j += 2) {
      if(dims[i] != other.dims[i] || bounds[j] != other.bounds[j] || bounds[j + 1] != bounds[j + 1]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a string representation of this unit that contains the intervals of
   * this unit.
   * 
   * @return a string representation of this unit
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(dims.length * 100);
    for(int i = 0; i < dims.length; i++) {
      result.append('d').append(dims[i]) //
          .append(":[").append(FormatUtil.NF4.format(bounds[i << 1])) //
          .append("; ").append(FormatUtil.NF4.format(bounds[(i << 1) + 1])).append(") ");
    }
    if(result.length() > 1) {
      result.setLength(result.length() - 1);
    }
    return result.toString();
  }
}
