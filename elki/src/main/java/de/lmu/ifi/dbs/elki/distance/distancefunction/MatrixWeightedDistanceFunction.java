package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.mahalanobisDistance;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Weighted distance for feature vectors.
 * 
 * @author Elke Achtert
 * @since 0.2
 */
// TODO: Factory with parameterizable weight matrix?
public class MatrixWeightedDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * The weight matrix.
   */
  protected double[][] weightMatrix;

  /**
   * Constructor.
   * 
   * @param weightMatrix weight matrix
   */
  public MatrixWeightedDistanceFunction(double[][] weightMatrix) {
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
  public VectorFieldTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, weightMatrix.length, weightMatrix.length);
  }
}
