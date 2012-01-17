package de.lmu.ifi.dbs.elki.math.linearalgebra;

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


/**
 * Simple class wrapping the result of a subspace projection.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Matrix
 */
public class SubspaceProjectionResult implements ProjectionResult {
  /**
   * The correlation dimensionality
   */
  private int correlationDimensionality;
  
  /**
   * The similarity matrix
   */
  private Matrix similarityMat;
  
  /**
   * Constructor.
   * 
   * @param correlationDimensionality dimensionality
   * @param similarityMat projection matrix
   */
  public SubspaceProjectionResult(int correlationDimensionality, Matrix similarityMat) {
    super();
    this.correlationDimensionality = correlationDimensionality;
    this.similarityMat = similarityMat;
  }

  @Override
  public int getCorrelationDimension() {
    return correlationDimensionality;
  }

  @Override
  public Matrix similarityMatrix() {
    return similarityMat;
  }
}
