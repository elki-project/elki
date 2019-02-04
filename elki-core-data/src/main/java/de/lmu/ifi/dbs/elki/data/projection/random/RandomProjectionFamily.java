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
package de.lmu.ifi.dbs.elki.data.projection.random;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Interface for random projection families.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - Projection
 */
public interface RandomProjectionFamily {
  /**
   * Generate a projection matrix for the given dimensionalities.
   *
   * @param idim Input Dimensionality
   * @param odim Output Dimensionality
   * @return Projection matrix
   */
  Projection generateProjection(int idim, int odim);

  /**
   * Interface for projection instances (<b>not thread safe</b>).
   *
   * To enable the use of internal buffers for faster multiplication, this code
   * is currently not thread safe.
   *
   * @author Erich Schubert
   */
  interface Projection {
    /**
     * Project a single vector.
     *
     * @param in Input vector
     * @return Projected vector
     */
    double[] project(NumberVector in);

    /**
     * Project a single vector, into the given buffer.
     *
     * @param in Input vector
     * @param buffer Output buffer
     * @return {@code buffer}
     */
    double[] project(NumberVector in, double[] buffer);

    /**
     * Get the output dimensionality.
     *
     * @return Output dimensionality
     */
    int getOutputDimensionality();
  }
}
