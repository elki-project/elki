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
package elki.math.statistics.dependence;

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.random.RandomFactory;
import elki.utils.containers.RankStruct;

import java.util.Arrays;
import java.util.Random;

/**
 * Implementation of bivariate Monte Carlo Density Estimation as described in
 * Edouard Fouché & Klemens Böhm<br>
 * Monte Carlo Density Estimation<br>
 * Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)
 * <p>
 * This is an abstract class. In order to use MCDE extend it and implement an appropriate statistical test that
 * returns a p-value and index structure for efficient computation of the statistical test.
 * <p>
 * The instantiation of MCDE based on the Mann-Whitney U test is called MWP (as described in the paper).
 * See McdeMwpDependenceMeasure.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

@Reference(authors = "Edouard Fouché, Klemens Böhm", //
    title = "Monte Carlo Density Estimation", //
    booktitle = "Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)", url = "https://doi.org/10.1145/3335783.3335795", //
    bibkey = "DBLP:conf/ssdbm/FoucheB19")

public abstract class MCDEDependenceMeasure<R extends RankStruct>
    extends AbstractDependenceMeasure {
  /**
   * Monte-Carlo iterations.
   */
  protected int m = 50;

  /**
   * Expected share of instances in slice (independent dimensions).
   */
  protected double alpha = 0.5;

  /**
   * Share of instances in marginal restriction (reference dimension).
   * Note that in the original paper alpha = beta and as such there is no explicit distinction between the parameters.
   */
  protected double beta = 0.5;

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param m     Monte-Carlo iterations
   * @param alpha Expected share of instances in slice (under independence)
   * @param beta  Share of instances in marginal restriction (reference dimension)
   * @param rnd   Random source
   */
  public MCDEDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd) {
    this.m = m;
    this.alpha = alpha;
    this.beta = beta;
    this.rnd = rnd;
  }

  /**
   * Overloaded wrapper for corrected_ranks()
   */
  protected <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
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
  protected abstract <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx);

  /**
   * Subclass must implement the computation of the statistical test, based on the slicing scheme.
   *
   * @param len             No of data instances
   * @param slice           An array of boolean resulting from a random slice
   * @param corrected_ranks the precomputed index structure for the reference dimension
   * @return a 1 - p-value
   */
  protected abstract double statistical_test(int len, boolean[] slice, R[] corrected_ranks);

  /**
   * Data Slicing
   *
   * @param len         No of data instances
   * @param nonRefIndex Index (see correctedRank()) computed for the dimension that is not the reference dimension
   * @return Array of booleans that states which instances are part of the slice
   */
  protected boolean[] randomSlice(int len, R[] nonRefIndex) {
    final Random random = rnd.getSingleThreadedRandom();
    boolean slice[] = new boolean[len];
    Arrays.fill(slice, true);

    // According to the actual formula it should be Math.ceil(Math.pow(this.alpha, 1.0) * len).
    // The exponent in the multivariate case should be the no. of dimensions - 1 which in the
    // bivariate case is always simply 1.
    final int slizeSize = (int) Math.ceil(this.alpha * len);
    final int start = random.nextInt(len - slizeSize);
    final int end = start + slizeSize;

    for(int j = 0; j < start; j++) {
      slice[nonRefIndex[j].index] = false;
    }

    for(int j = end; j < len; j++) {
      slice[(int) nonRefIndex[j].index] = false;
    }

    return slice;
  }

  /**
   * Implements dependence from DependenceMeasure superclass. Corresponds to Algorithm 4 in source paper.
   *
   * @param adapter1 First data adapter
   * @param data1    First data set
   * @param adapter2 Second data adapter
   * @param data2    Second data set
   * @param <A>      Numeric data type, such as double
   * @param <B>      Numeric data type, such as double
   * @return MCDE result
   */
  @Override public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2) {
    final Random random = rnd.getSingleThreadedRandom();
    final int len = adapter1.size(data1);

    if(len != adapter2.size(data2)) {
      throw new AbortException("Size of both arrays must match!");
    }

    final R[] index_0 = corrected_ranks(adapter1, data1, len);
    final R[] index_1 = corrected_ranks(adapter2, data2, len);

    double mwp = 0;

    for(int i = 0; i < this.m; i++) {
      int r = random.nextInt(2);
      R[] ref_index;
      R[] other_index;

      if(r == 1) {
        ref_index = index_1;
        other_index = index_0;
      }
      else {
        ref_index = index_0;
        other_index = index_1;
      }

      mwp += statistical_test(len, randomSlice(len, other_index), ref_index);
    }
    return mwp / m;
  }

}
