package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
 * Interface for random projection families.
 * 
 * @author Erich Schubert
 */
public interface RandomProjectionFamily {
  /**
   * Generate a projection matrix for the given dimensionalities.
   * 
   * @param dim Input Dimensionality
   * @param odim Output Dimensionality
   * @return Projection matrix
   */
  Matrix generateProjectionMatrix(int dim, int odim);

  /**
   * Generate a random projection vector for the given dimensionality.
   * 
   * @param dim Input Dimensionality
   * @return Projection vector
   */
  Vector generateProjectionVector(int dim);
}
