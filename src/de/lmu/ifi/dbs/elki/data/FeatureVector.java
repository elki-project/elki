package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.List;

/**
 * Generic FeatureVector class that can contain any type of data (i.e. numerical
 * or categorical attributes). See {@link NumberVector} for vectors that
 * actually store numerical features.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector class
 * @param <D> Data type
 */
public interface FeatureVector<V extends FeatureVector<? extends V, D>, D> {
  /**
   * The dimensionality of the vector space where of this FeatureVector of V is
   * an element.
   * 
   * @return the number of dimensions of this FeatureVector of V
   */
  int getDimensionality();

  /**
   * Returns the value in the specified dimension.
   * 
   * @param dimension the desired dimension, where 1 &le; dimension &le;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  D getValue(int dimension);

  /**
   * Returns a String representation of the FeatureVector of V as a line that is
   * suitable to be printed in a sequential file.
   * 
   * @return a String representation of the FeatureVector of V
   */
  @Override
  String toString();

  /**
   * Returns a new FeatureVector of V for the given values.
   * 
   * @param values the values of the featureVector
   * @return a new FeatureVector of V for the given values
   */
  V newInstance(D[] values);

  /**
   * Returns a new FeatureVector of V for the given values.
   * 
   * @param values the values of the featureVector
   * @return a new FeatureVector of V for the given values
   */
  V newInstance(List<D> values);
}