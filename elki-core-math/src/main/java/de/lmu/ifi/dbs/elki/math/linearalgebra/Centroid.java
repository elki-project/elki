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

/**
 * Class to compute the centroid of some data.
 * 
 * This is a more numerically stable approach than simply taking the sum divided
 * by the count.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class Centroid implements NumberVector {
  /**
   * The current weight.
   */
  protected double wsum;

  /**
   * Vector elements.
   */
  protected double[] elements;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   */
  public Centroid(int dim) {
    super();
    this.elements = new double[dim];
    this.wsum = 0;
  }

  /**
   * Add a single value with weight 1.0.
   * 
   * @param val Value
   */
  public void put(double[] val) {
    assert (val.length == elements.length);
    wsum += 1.0;
    for(int i = 0; i < elements.length; i++) {
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
  public void put(double[] val, double weight) {
    assert (val.length == elements.length);
    if(weight == 0) {
      return; // Skip zero weights.
    }
    final double nwsum = weight + wsum;
    for(int i = 0; i < elements.length; i++) {
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
  public void put(NumberVector val) {
    assert (val.getDimensionality() == elements.length);
    wsum += 1.0;
    for(int i = 0; i < elements.length; i++) {
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
  public void put(NumberVector val, double weight) {
    assert (val.getDimensionality() == elements.length);
    if(weight == 0) {
      return; // Skip zero weights.
    }
    final double nwsum = weight + wsum;
    for(int i = 0; i < elements.length; i++) {
      final double delta = val.doubleValue(i) - elements[i];
      final double rval = delta * weight / nwsum;
      elements[i] += rval;
    }
    wsum = nwsum;
  }

  @Override
  public double doubleValue(int dimension) {
    return elements[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return (long) elements[dimension];
  }

  @Override
  public int getDimensionality() {
    return elements.length;
  }

  @Override
  public double[] toArray() {
    return elements.clone();
  }

  /**
   * Static constructor from an existing relation.
   * 
   * @param relation Relation to use
   * @param ids IDs to use
   * @return Centroid
   */
  public static Centroid make(Relation<? extends NumberVector> relation, DBIDs ids) {
    final int dim = RelationUtil.dimensionality(relation);
    Centroid c = new Centroid(dim);
    double[] elems = c.elements;
    int count = 0;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      NumberVector v = relation.get(iter);
      for(int i = 0; i < dim; i++) {
        elems[i] += v.doubleValue(i);
      }
      count += 1;
    }
    if(count == 0) {
      return c;
    }
    for(int i = 0; i < dim; i++) {
      elems[i] /= count;
    }
    c.wsum = count;
    return c;
  }

  /**
   * Low-level access to the element array.
   * 
   * @return Array access
   */
  public double[] getArrayRef() {
    return elements;
  }
}
