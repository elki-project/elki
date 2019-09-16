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
package elki.math.statistics.tests.mcde;

import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

/**
 * Abstract class for statistical tests for MCDEDependenceMeasure.
 * See MWPTest for an example. Implementation should contain an appropriate index
 * structure to efficiently compute statistical test and the test itself.
 *
 * @param <R> RankStruct or Subclass of RankStruct
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

public abstract class MCDETest<R extends MCDETest.RankStruct> {

  /**
   * Structure to hold return values in index creation for MCDEDependenceEstimate
   */
  public class RankStruct {
    public int index;

    public RankStruct(int index) {
      this.index = index;
    }
  }

  /**
   * Build a sorted index of objects.
   *
   * @param adapter Data adapter
   * @param data    Data array
   * @param len     Length of data
   * @return Sorted index
   */
  protected <A> int[] sortedIndex(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    int[] s1 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, (x, y) -> Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y)));
    return s1;
  }

  /**
   * Overloaded wrapper for corrected_ranks()
   */
  public <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    return corrected_ranks(adapter, data, sortedIndex(adapter, data, len));
  }

  /**
   * Subclass must implement computation of corrected rank index.
   *
   * @param adapter ELKI NumberArrayAdapter Subclass
   * @param data    One dimensional array containing one dimension of the data
   * @param idx     Return value of sortedIndex()
   * @return Array of RankStruct, acting as rank index
   */
  abstract public <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx);

  /**
   * Subclass must implement the computation of the statistical test, based on the slicing scheme
   * of MCDEDependenceMeasure.
   *
   * @param start           Starting index value for statistical test
   * @param end             End index value for statistical test
   * @param slice           An array of boolean resulting from a random slice
   * @param corrected_ranks the precomputed index structure for the reference dimension
   * @return a 1 - p-value
   */
  abstract public double statistical_test(int start, int end, boolean[] slice, R[] corrected_ranks);
}
