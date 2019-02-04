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

import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Random projection family based on selecting random features.
 * <p>
 * The basic idea of using this for data mining should probably be attributed to
 * L. Breiman, who used it to improve the performance of predictors in an
 * ensemble.
 * <p>
 * Reference:
 * <p>
 * L. Breiman<br>
 * Bagging predictors<br>
 * Machine learning 24.2
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "L. Breiman", //
    title = "Bagging predictors", //
    booktitle = "Machine learning 24.2", //
    url = "https://doi.org/10.1007/BF00058655", //
    bibkey = "DBLP:journals/ml/Breiman96b")
@Alias("de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.RandomSubsetProjectionFamily")
public class RandomSubsetProjectionFamily extends AbstractRandomProjectionFamily {
  /**
   * Constructor.
   *
   * @param random Random generator.
   */
  public RandomSubsetProjectionFamily(RandomFactory random) {
    super(random);
  }

  @Override
  public Projection generateProjection(int idim, int odim) {
    int[] dims;
    if(odim < idim) {
      dims = Arrays.copyOf(randomPermutation(sequence(idim), random), odim);
    }
    else if(odim == idim) {
      dims = randomPermutation(sequence(idim), random);
    }
    else {
      final int mdim = ((odim - 1) / idim + 1) * idim;
      dims = new int[mdim];
      for(int i = 0, j = 0; i < mdim; i++) {
        dims[i] = j++;
        j = j == idim ? 0 : j;
      }
      dims = Arrays.copyOf(randomPermutation(dims, random), odim);
    }
    return new SubsetProjection(dims);
  }

  /**
   * Perform a random permutation of the array, in-place.
   *
   * Knuth / Fisher-Yates style shuffle
   *
   * TODO: move to shared code.
   *
   * @param out Existing output array
   * @param random Random generator.
   * @return Same array.
   */
  public static int[] randomPermutation(final int[] out, Random random) {
    for(int i = out.length - 1; i > 0; i--) {
      // Swap with random preceeding element.
      int ri = random.nextInt(i + 1);
      int tmp = out[ri];
      out[ri] = out[i];
      out[i] = tmp;
    }
    return out;
  }

  /**
   * Generate the integer sequence 0 .. e-1
   * <p>
   * TODO: move to shared code.
   *
   * @param e End (exclusive)
   * @return End
   */
  private static int[] sequence(int e) {
    int[] a = new int[e];
    for(int i = 0; i < a.length; i++) {
      a[i] = i;
    }
    return a;
  }

  /**
   * Random subset projection.
   *
   * @author Erich Schubert
   */
  public static class SubsetProjection implements Projection {
    /**
     * Input dimensions.
     */
    private int[] dims;

    /**
     * Constructor.
     *
     * @param dims Data permutation.
     */
    public SubsetProjection(int[] dims) {
      this.dims = dims;
    }

    @Override
    public double[] project(NumberVector in) {
      return project(in, new double[dims.length]);
    }

    @Override
    public double[] project(NumberVector in, double[] buffer) {
      for(int i = 0; i < dims.length; i++) {
        buffer[i] = in.doubleValue(dims[i]);
      }
      return buffer;
    }

    @Override
    public int getOutputDimensionality() {
      return dims.length;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected RandomSubsetProjectionFamily makeInstance() {
      return new RandomSubsetProjectionFamily(random);
    }
  }
}
