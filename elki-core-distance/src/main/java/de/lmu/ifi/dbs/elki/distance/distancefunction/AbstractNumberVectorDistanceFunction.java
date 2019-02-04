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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * Abstract base class for the most common family of distance functions: defined
 * on number vectors and returning double values.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @assoc - - - NumberVector
 */
public abstract class AbstractNumberVectorDistanceFunction implements NumberVectorDistanceFunction<NumberVector> {
  /**
   * Constructor.
   */
  public AbstractNumberVectorDistanceFunction() {
    super();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.FIELD;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(SpatialComparable o1, SpatialComparable o2) {
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    if(dim1 != dim2) {
      throw new IllegalArgumentException("Objects do not have the same dimensionality.");
    }
    return dim1;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(double[] o1, SpatialComparable o2) {
    final int dim1 = o1.length, dim2 = o2.getDimensionality();
    if(dim1 != dim2) {
      throw new IllegalArgumentException("Objects do not have the same dimensionality.");
    }
    return dim1;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(double[] o1, double[] o2) {
    final int dim1 = o1.length, dim2 = o2.length;
    if(dim1 != dim2) {
      throw new IllegalArgumentException("Objects do not have the same dimensionality.");
    }
    return dim1;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @param expect Expected dimensionality
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(SpatialComparable o1, SpatialComparable o2, int expect) {
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    if(dim1 != dim2 || dim1 != expect) {
      throw new IllegalArgumentException("Objects do not have the expected dimensionality of " + expect);
    }
    return expect;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @param expect Expected dimensionality
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(double[] o1, SpatialComparable o2, int expect) {
    final int dim1 = o1.length, dim2 = o2.getDimensionality();
    if(dim1 != dim2 || dim1 != expect) {
      throw new IllegalArgumentException("Objects do not have the expected dimensionality of " + expect);
    }
    return expect;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @param expect Expected dimensionality
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(double[] o1, double[] o2, int expect) {
    final int dim1 = o1.length, dim2 = o2.length;
    if(dim1 != dim2 || dim1 != expect) {
      throw new IllegalArgumentException("Objects do not have the expected dimensionality of " + expect);
    }
    return expect;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(NumberVector o1, NumberVector o2) {
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    if(dim1 != dim2) {
      throw new IllegalArgumentException("Objects do not have the same dimensionality.");
    }
    return dim1;
  }

  /**
   * Get the common dimensionality of the two objects. Throw an
   * {@link IllegalArgumentException} otherwise.
   * 
   * @param o1 First vector / MBR
   * @param o2 Second vector / MBR
   * @param expect Expected dimensionality
   * @return Common dimensionality
   * @throws IllegalArgumentException when dimensionalities are not the same.
   */
  public static final int dimensionality(NumberVector o1, NumberVector o2, int expect) {
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    if(dim1 != dim2 || dim1 != expect) {
      throw new IllegalArgumentException("Objects do not have the expected dimensionality of " + expect);
    }
    return expect;
  }
}
