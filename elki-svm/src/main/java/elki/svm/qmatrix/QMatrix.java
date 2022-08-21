/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.svm.qmatrix;

/**
 * API to get kernel similarity values.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public interface QMatrix {
  /**
   * Get a column of the matrix.
   * 
   * @param column Column number
   * @param len Number of entries to get
   * @param out Output array for similarity values
   */
  default void get_Q(int column, int len, float[] out) {
    if(out == null) {
      return; // Used to pre-cache values
    }
    for(int j = 0; j < len; ++j) {
      out[j] = (float) similarity(column, j);
    }
  }

  /**
   * Get the diagonal values, as reference.
   * 
   * @return Diagonal values
   */
  double[] get_QD();

  /**
   * Reorganize the data by swapping two entries.
   * <p>
   * This also must modify the QD array!
   * 
   * @param i First entry
   * @param j Second entry
   */
  void swap_index(int i, int j);

  /**
   * (Slow) compute the similarity (not distance) of objects i and j.
   * <p>
   * If you need many, use {@link #get_Q(int, int, float[])} instead.
   *
   * @param i First object
   * @param j Second object
   * @return Similarity
   */
  double similarity(int i, int j);

  /**
   * Initialize the Q Matrix.
   */
  void initialize();
}
