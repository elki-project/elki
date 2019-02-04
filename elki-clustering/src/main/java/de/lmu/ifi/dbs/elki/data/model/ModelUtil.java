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
package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberVectorAdapter;

/**
 * Utility classes for dealing with cluster models.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @assoc - - - Model
 * @assoc - - - NumberVector
 */
public final class ModelUtil {
  /**
   * Private constructor. Static methods only.
   */
  private ModelUtil() {
    // Do not use.
  }

  /**
   * Get (and convert!) the representative vector for a cluster model.
   * 
   * <b>Only representative-based models are supported!</b>
   * 
   * {@code null} is returned when the model is not supported!
   * 
   * @param model Model
   * @param relation Data relation (for representatives specified per DBID)
   * @param factory Vector factory, for type conversion.
   * @return Vector of type V, {@code null} if not supported.
   * @param <V> desired vector type
   */
  @SuppressWarnings("unchecked")
  public static <V extends NumberVector> V getPrototype(Model model, Relation<? extends V> relation, NumberVector.Factory<V> factory) {
    // Mean model contains a numeric Vector
    if(model instanceof MeanModel) {
      final double[] p = ((MeanModel) model).getMean();
      return factory.newNumberVector(p);
    }
    // Handle medoid models
    if(model instanceof MedoidModel) {
      NumberVector p = relation.get(((MedoidModel) model).getMedoid());
      if(factory.getRestrictionClass().isInstance(p)) {
        return (V) p;
      }
      return factory.newNumberVector(p, NumberVectorAdapter.STATIC);
    }
    if(model instanceof PrototypeModel) {
      Object p = ((PrototypeModel<?>) model).getPrototype();
      if(factory.getRestrictionClass().isInstance(p)) {
        return (V) p;
      }
      if(p instanceof NumberVector) {
        return factory.newNumberVector((NumberVector) p, NumberVectorAdapter.STATIC);
      }
      return null; // Inconvertible
    }
    return null;
  }

  /**
   * Get the representative vector for a cluster model.
   * 
   * <b>Only representative-based models are supported!</b>
   * 
   * {@code null} is returned when the model is not supported!
   * 
   * @param model Model
   * @param relation Data relation (for representatives specified per DBID)
   * @return Some {@link NumberVector}, {@code null} if not supported.
   */
  public static NumberVector getPrototype(Model model, Relation<? extends NumberVector> relation) {
    // Mean model contains a numeric Vector
    if(model instanceof MeanModel) {
      return DoubleVector.wrap(((MeanModel) model).getMean());
    }
    // Handle medoid models
    if(model instanceof MedoidModel) {
      return relation.get(((MedoidModel) model).getMedoid());
    }
    if(model instanceof PrototypeModel) {
      Object p = ((PrototypeModel<?>) model).getPrototype();
      if(p instanceof NumberVector) {
        return (NumberVector) p;
      }
      return null; // Inconvertible
    }
    return null;
  }

  /**
   * Get the representative vector for a cluster model, or compute the centroid.
   *
   * @param model Model
   * @param relation Data relation (for representatives specified per DBID)
   * @param ids Cluster ids (must not be empty.
   * @return Vector of type V, {@code null} if not supported.
   * @param <V> desired vector type
   */
  public static <V extends NumberVector> V getPrototypeOrCentroid(Model model, Relation<? extends V> relation, DBIDs ids, NumberVector.Factory<V> factory) {
    assert (ids.size() > 0);
    V v = getPrototype(model, relation, factory);
    return v != null ? v : factory.newNumberVector(Centroid.make(relation, ids));
  }

  /**
   * Get the representative vector for a cluster model, or compute the centroid.
   *
   * @param model Model
   * @param relation Data relation (for representatives specified per DBID)
   * @param ids Cluster ids (must not be empty.
   * @return Some {@link NumberVector}.
   */
  public static NumberVector getPrototypeOrCentroid(Model model, Relation<? extends NumberVector> relation, DBIDs ids) {
    assert (ids.size() > 0);
    NumberVector v = getPrototype(model, relation);
    return v != null ? v : Centroid.make(relation, ids);
  }
}
