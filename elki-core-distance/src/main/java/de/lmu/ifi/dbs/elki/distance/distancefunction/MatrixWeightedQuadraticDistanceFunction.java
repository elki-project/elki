/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.mahalanobisDistance;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimesTimes;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Matrix weighted quadratic distance, the squared form of
 * {@link MahalanobisDistanceFunction}.
 *
 * For a weight matrix M, this distance is defined as
 * \[ \text{Mahalanobis}^2_M(\vec{x},\vec{y}) := (\vec{x}-\vec{y})^T * M *
 * (\vec{x}-\vec{y}) \]
 * 
 * TODO: Add a factory with parameterizable weight matrix! Right now, this can
 * only be used from Java and from subclasses, not from command line or MiniGUI.
 *
 * @author Elke Achtert
 * @since 0.2
 */
public class MatrixWeightedQuadraticDistanceFunction extends AbstractNumberVectorDistanceFunction implements Norm<NumberVector> {
  /**
   * The weight matrix.
   */
  protected double[][] weightMatrix;

  /**
   * Constructor.
   * 
   * @param weightMatrix weight matrix
   */
  public MatrixWeightedQuadraticDistanceFunction(double[][] weightMatrix) {
    super();
    this.weightMatrix = weightMatrix;
    assert (weightMatrix[0].length == weightMatrix.length);
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    dimensionality(o1, o2, weightMatrix.length);
    return mahalanobisDistance(weightMatrix, o1.toArray(), o2.toArray());
  }

  @Override
  public double norm(NumberVector obj) {
    double[] v = obj.toArray();
    return transposeTimesTimes(v, weightMatrix, v);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof MatrixWeightedQuadraticDistanceFunction && //
        VMath.equals(this.weightMatrix, ((MatrixWeightedQuadraticDistanceFunction) obj).weightMatrix));
  }

  // TODO: fairly expensive - cache the hash code?
  @Override
  public int hashCode() {
    return this.getClass().hashCode() * 31 + Arrays.deepHashCode(weightMatrix);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, weightMatrix.length, weightMatrix.length);
  }
}
