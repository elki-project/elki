package de.lmu.ifi.dbs.elki.database.relation;

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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Utility functions for handling database relation.
 * 
 * @author Erich Schubert
 */
public final class RelationUtil {
  /**
   * Fake constructor: do not instantiate.
   */
  private RelationUtil() {
    // Do not instantiate!
  }

  /**
   * Get the vector field type information from a relation.
   * 
   * @param relation relation
   * @param <V> Vector type
   * @return Vector field type information
   */
  public static <V extends FeatureVector<?>> VectorFieldTypeInformation<V> assumeVectorField(Relation<V> relation) {
    try {
      return ((VectorFieldTypeInformation<V>) relation.getDataTypeInformation());
    } catch (Exception e) {
      throw new UnsupportedOperationException("Expected a vector field, got type information: " + relation.getDataTypeInformation().toString(), e);
    }
  }

  /**
   * Get the number vector factory of a database relation.
   * 
   * @param relation relation
   * @param <V> Vector type
   * @param <N> Number type
   * @return Vector field type information
   */
  public static <V extends NumberVector<? extends N>, N extends Number> NumberVector.Factory<V, N> getNumberVectorFactory(Relation<V> relation) {
    final VectorFieldTypeInformation<V> type = assumeVectorField(relation);
    @SuppressWarnings("unchecked")
    final NumberVector.Factory<V, N> factory = (NumberVector.Factory<V, N>) type.getFactory();
    return factory;
  }

  /**
   * Get the dimensionality of a database relation.
   * 
   * @param relation relation
   * @return Database dimensionality
   */
  public static int dimensionality(Relation<? extends FeatureVector<?>> relation) {
    try {
      return ((VectorFieldTypeInformation<? extends FeatureVector<?>>) relation.getDataTypeInformation()).dimensionality();
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Get the column name or produce a generic label "Column XY".
   * 
   * @param rel Relation
   * @param col Column
   * @param <V> Vector type
   * @return Label
   */
  public static <V extends FeatureVector<?>> String getColumnLabel(Relation<? extends V> rel, int col) {
    String lbl = assumeVectorField(rel).getLabel(col);
    if (lbl != null) {
      return lbl;
    } else {
      return "Column " + col;
    }
  }
}
