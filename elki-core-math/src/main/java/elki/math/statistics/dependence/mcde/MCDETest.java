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
package elki.math.statistics.dependence.mcde;

import elki.math.statistics.dependence.MCDEDependence;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Interface for statistical tests for {@link MCDEDependence}.
 * See {@link MWPTest} for an example. Implementation should contain an
 * appropriate index structure to efficiently compute statistical test and the
 * test itself.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 * @since 0.8.0
 *
 * @param <R> RankStruct or Subclass of RankStruct
 */
public interface MCDETest<R extends MCDETest.RankStruct> {
  /**
   * Structure to hold return values in index creation for MCDEDependence
   *
   * @author Alan Mazankiewicz
   * @author Edouard Fouché
   */
  public class RankStruct {
    /**
     * Position information
     */
    public int[] index;

    /**
     * Constructor.
     *
     * @param idx Array containing index (position) values
     */
    public RankStruct(int[] idx) {
      this.index = idx;
    }
  }

  /**
   * Compute the corrected rank index.
   *
   * @param <A> Input array type
   * @param adapter Array-like adapter
   * @param data Data object
   * @param len Length
   * @return corrected ranks
   */
  <A> R correctedRanks(final NumberArrayAdapter<?, A> adapter, final A data, int len);

  /**
   * Subclass must implement the computation of the statistical test, based on
   * the slicing scheme of MCDEDependence.
   *
   * @param start Starting index value for statistical test
   * @param width Width of the slice (endindex = start + width)
   * @param slice An array of boolean resulting from a random slice
   * @param correctedRanks the precomputed index for the reference dimension
   * @return a 1 - p-value
   */
  double statisticalTest(int start, int width, boolean[] slice, R correctedRanks);
}
