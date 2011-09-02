package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Provides the Weighted distance for feature vectors.
 * 
 * @author Elke Achtert
 */
// TODO: Factory with parameterizable weight matrix?
public class WeightedDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * The weight matrix.
   */
  protected Matrix weightMatrix;

  /**
   * Provides the Weighted distance for feature vectors.
   * 
   * @param weightMatrix weight matrix
   */
  public WeightedDistanceFunction(Matrix weightMatrix) {
    super();
    this.weightMatrix = weightMatrix;
    assert (weightMatrix.getColumnDimensionality() == weightMatrix.getRowDimensionality());
  }

  /**
   * Provides the Weighted distance for feature vectors.
   * 
   * @return the Weighted distance between the given two vectors
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }

    Vector o1_minus_o2 = o1.getColumnVector().minus(o2.getColumnVector());
    double dist = MathUtil.mahalanobisDistance(weightMatrix, o1_minus_o2);

    return dist;
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return VectorFieldTypeInformation.get(NumberVector.class, weightMatrix.getColumnDimensionality());
  }
}