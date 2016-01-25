package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
  protected Matrix weightMatrix;

  /**
   * Constructor.
   * 
   * @param weightMatrix weight matrix
   */
  public MatrixWeightedDistanceFunction(Matrix weightMatrix) {
    super();
    this.weightMatrix = weightMatrix;
    assert (weightMatrix.getColumnDimensionality() == weightMatrix.getRowDimensionality());
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    dimensionality(o1, o2, weightMatrix.getColumnDimensionality());
    Vector o1_minus_o2 = o1.getColumnVector().minusEquals(o2.getColumnVector());
    return MathUtil.mahalanobisDistance(weightMatrix, o1_minus_o2);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, weightMatrix.getColumnDimensionality(), weightMatrix.getColumnDimensionality());
  }
}
