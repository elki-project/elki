package de.lmu.ifi.dbs.elki.math.linearalgebra;

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


/**
 * Interface representing a simple projection result.
 * 
 * This can either come from a full PCA, or just from an axis-parallel subspace selection
 * 
 * @author Erich Schubert
 */
// TODO: cleanup
public interface ProjectionResult {
  /**
   * Get the number of "strong" dimensions
   * 
   * @return number of strong (correlated) dimensions
   */
  public int getCorrelationDimension();
  
  /**
   * Projection matrix
   * 
   * @return projection matrix
   */
  public Matrix similarityMatrix();
}
