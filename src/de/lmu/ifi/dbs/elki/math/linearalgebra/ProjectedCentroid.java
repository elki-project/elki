package de.lmu.ifi.dbs.elki.math.linearalgebra;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * Centroid only using a subset of dimensions.
 * 
 * This class abstracts the mathematics of efficient and numerically stable
 * computation of projected centroids.
 * 
 * See {@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil DatabaseUtil} for
 * easier to use APIs.
 * 
 * @author Erich Schubert
 */
public class ProjectedCentroid extends Centroid {
  /**
   * The selected dimensions.
   */
  private BitSet dims;

  /**
   * Constructor for updating use.
   * 
   * @param dims Dimensions to use (indexed with 0)
   * @param dim Full dimensionality
   */
  public ProjectedCentroid(BitSet dims, int dim) {
    super(dim);
    this.dims = dims;
    assert (dims.size() <= dim);
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(double[] val) {
    assert (val.length == elements.length);
    wsum += 1.0;
    for(int i = dims.nextSetBit(0); i >= 0; i = dims.nextSetBit(i + 1)) {
      final double delta = val[i] - elements[i];
      elements[i] += delta / wsum;
    }
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  @Override
  public void put(double val[], double weight) {
    assert (val.length == elements.length);
    final double nwsum = weight + wsum;
    for(int i = dims.nextSetBit(0); i >= 0; i = dims.nextSetBit(i + 1)) {
      final double delta = val[i] - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(NumberVector<?, ?> val) {
    assert (val.getDimensionality() == elements.length);
    wsum += 1.0;
    for(int i = dims.nextSetBit(0); i >= 0; i = dims.nextSetBit(i + 1)) {
      final double delta = val.doubleValue(i + 1) - elements[i];
      elements[i] += delta / wsum;
    }
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  @Override
  public void put(NumberVector<?, ?> val, double weight) {
    assert (val.getDimensionality() == elements.length);
    final double nwsum = weight + wsum;
    for(int i = dims.nextSetBit(0); i >= 0; i = dims.nextSetBit(i + 1)) {
      final double delta = val.doubleValue(i + 1) - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  /**
   * Static Constructor from a relation.
   * 
   * @param dims Dimensions to use (indexed with 0)
   * @param relation Relation to process
   */
  public static ProjectedCentroid make(BitSet dims, Relation<? extends NumberVector<?, ?>> relation) {
    ProjectedCentroid c = new ProjectedCentroid(dims, DatabaseUtil.dimensionality(relation));
    assert (dims.size() <= DatabaseUtil.dimensionality(relation));
    for(DBID id : relation.iterDBIDs()) {
      c.put(relation.get(id));
    }
    return c;
  }

  /**
   * Static Constructor from a relation.
   * 
   * @param dims Dimensions to use (indexed with 0)
   * @param relation Relation to process
   * @param ids IDs to process
   */
  public static ProjectedCentroid make(BitSet dims, Relation<? extends NumberVector<?, ?>> relation, Iterable<DBID> ids) {
    ProjectedCentroid c = new ProjectedCentroid(dims, DatabaseUtil.dimensionality(relation));
    assert (dims.size() <= DatabaseUtil.dimensionality(relation));
    for(DBID id : ids) {
      c.put(relation.get(id));
    }
    return c;
  }
}