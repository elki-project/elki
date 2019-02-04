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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * Centroid only using a subset of dimensions.
 * 
 * This class abstracts the mathematics of efficient and numerically stable
 * computation of projected centroids.
 * 
 * See {@link de.lmu.ifi.dbs.elki.database.DatabaseUtil DatabaseUtil} for
 * easier to use APIs.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class ProjectedCentroid extends Centroid {
  /**
   * The selected dimensions.
   */
  private long[] dims;

  /**
   * Constructor for updating use.
   * 
   * @param dims Dimensions to use (indexed with 0)
   * @param dim Full dimensionality
   */
  public ProjectedCentroid(long[] dims, int dim) {
    super(dim);
    this.dims = dims;
  }

  /**
   * Add a single value with weight 1.0.
   * 
   * @param val Value
   */
  @Override
  public void put(double[] val) {
    assert (val.length == elements.length);
    wsum += 1.0;
    for(int i = BitsUtil.nextSetBit(dims, 0); i >= 0; i = BitsUtil.nextSetBit(dims, i + 1)) {
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
  public void put(double[] val, double weight) {
    assert (val.length == elements.length);
    if(weight == 0) {
      return; // Skip zero weights.
    }
    final double nwsum = weight + wsum;
    for(int i = BitsUtil.nextSetBit(dims, 0); i >= 0; i = BitsUtil.nextSetBit(dims, i + 1)) {
      final double delta = val[i] - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  /**
   * Add a single value with weight 1.0.
   * 
   * @param val Value
   */
  @Override
  public void put(NumberVector val) {
    assert (val.getDimensionality() == elements.length);
    wsum += 1.0;
    for(int i = BitsUtil.nextSetBit(dims, 0); i >= 0; i = BitsUtil.nextSetBit(dims, i + 1)) {
      final double delta = val.doubleValue(i) - elements[i];
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
  public void put(NumberVector val, double weight) {
    assert (val.getDimensionality() == elements.length);
    if(weight == 0) {
      return; // Skip zero weights.
    }
    final double nwsum = weight + wsum;
    for(int i = BitsUtil.nextSetBit(dims, 0); i >= 0; i = BitsUtil.nextSetBit(dims, i + 1)) {
      final double delta = val.doubleValue(i) - elements[i];
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
   * @return Centroid
   */
  public static ProjectedCentroid make(long[] dims, Relation<? extends NumberVector> relation) {
    ProjectedCentroid c = new ProjectedCentroid(dims, RelationUtil.dimensionality(relation));
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      c.put(relation.get(iditer));
    }
    return c;
  }

  /**
   * Static Constructor from a relation.
   * 
   * @param dims Dimensions to use (indexed with 0)
   * @param relation Relation to process
   * @param ids IDs to process
   * @return Centroid
   */
  public static ProjectedCentroid make(long[] dims, Relation<? extends NumberVector> relation, DBIDs ids) {
    ProjectedCentroid c = new ProjectedCentroid(dims, RelationUtil.dimensionality(relation));
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      c.put(relation.get(iter));
    }
    return c;
  }
}