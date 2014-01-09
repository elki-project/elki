package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

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
import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random projection family based on selecting random features.
 * 
 * The basic idea of using this for data mining should probably be attributed to
 * L. Breiman, who used it to improve the performance of predictors in an
 * ensemble.
 * 
 * Reference:
 * <p>
 * L. Breiman<br />
 * Bagging predictors<br />
 * Machine learning 24.2
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "L. Breiman", title = "Bagging predictors", booktitle = "Machine learning 24.2", url = "http://dx.doi.org/10.1007/BF00058655")
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
    if (odim < idim) {
      dims = Arrays.copyOf(randomPermutation(range(0, idim), random), odim);
    } else if (odim == idim) {
      dims = randomPermutation(range(0, idim), random);
    } else {
      int mdim = idim;
      while (mdim < odim) {
        mdim += idim;
      }
      dims = new int[mdim];
      for (int i = 0; i < mdim; i++) {
        dims[i] = i % idim;
      }
      dims = Arrays.copyOf(randomPermutation(dims, random), odim);
    }
    return new SubsetProjection(dims);
  }

  /**
   * Initialize an integer value range.
   * 
   * FIXME: move to shared code.
   * 
   * @param start Starting value
   * @param end End value (exclusive)
   * @return Array of integers start..end, excluding end.
   */
  public static int[] range(int start, int end) {
    int[] out = new int[end - start];
    for (int i = 0, j = start; j < end; i++, j++) {
      out[i] = j;
    }
    return out;
  }

  /**
   * Perform a random permutation of the array, in-place.
   * 
   * Knuth / Fisher-Yates style shuffle
   * 
   * FIXME: move to shared code.
   * 
   * @param out Existing output array
   * @param random Random generator.
   * @return Same array.
   */
  public static int[] randomPermutation(final int[] out, Random random) {
    for (int i = out.length - 1; i > 0; i--) {
      // Swap with random preceeding element.
      int ri = random.nextInt(i + 1);
      int tmp = out[ri];
      out[ri] = out[i];
      out[i] = tmp;
    }
    return out;
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
    public double[] project(NumberVector<?> in) {
      double[] buf = new double[dims.length];
      for (int i = 0; i < dims.length; i++) {
        buf[i] = in.doubleValue(dims[i]);
      }
      return buf;
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected RandomSubsetProjectionFamily makeInstance() {
      return new RandomSubsetProjectionFamily(random);
    }
  }
}
