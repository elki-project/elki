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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Random hyperplane projection family.
 * <p>
 * Reference:
 * <p>
 * M. Henzinger<br>
 * Finding near-duplicate web pages: a large-scale evaluation of algorithms<br>
 * Proc. 29th ACM Conf. Research and Development in Information Retrieval
 * (SIGIR 2006)
 * <p>
 * TODO: Benchmark if {@code boolean[][]} and "if" is faster, or multiplication
 * (does Java emit SIMD code then?)
 *
 * @author Evgeniy Faerman
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "M. Henzinger", //
    title = "Finding near-duplicate web pages: a large-scale evaluation of algorithms", //
    booktitle = "Proc. 29th ACM Conf. Research and Development in Information Retrieval (SIGIR 2006)", //
    url = "https://doi.org/10.1145/1148170.1148222", //
    bibkey = "DBLP:conf/sigir/Henzinger06")
@Alias("de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.SimplifiedRandomHyperplaneProjectionFamily")
public class SimplifiedRandomHyperplaneProjectionFamily implements RandomProjectionFamily {
  /**
   * Random generator.
   */
  protected Random random;

  /**
   * Constructor.
   *
   * @param random Random number generator.
   */
  public SimplifiedRandomHyperplaneProjectionFamily(RandomFactory random) {
    super();
    this.random = random.getSingleThreadedRandom();
  }

  @Override
  public Projection generateProjection(int dim, int k) {
    return new SignedProjection(dim, k, random);
  }

  /**
   * Fast projection class, using booleans to represent +-1 matrix entries.
   *
   * Optimized for sparse multiplications, thus we are using a column-row
   * layout.
   *
   * @author Erich Schubert
   */
  private static class SignedProjection implements Projection {
    /**
     * Projection matrix.
     */
    boolean[][] mat;

    /**
     * Output dimensionality
     */
    private int k;

    /**
     * Shared buffer to use during projections.
     */
    private double[] buf;

    /**
     * Constructor.
     *
     * @param dim Input dimensionality
     * @param k Output dimensionality
     * @param random Random generator
     */
    public SignedProjection(int dim, int k, Random random) {
      mat = new boolean[dim][k];
      for(int i = 0; i < dim; ++i) {
        final boolean[] row = mat[i];
        for(int j = 0; j < k; ++j) {
          row[j] = random.nextBoolean();
        }
      }
      this.k = k;
      this.buf = new double[dim];
    }

    @Override
    public double[] project(NumberVector in) {
      return project(in, new double[k]);
    }

    @Override
    public double[] project(NumberVector vec, double[] ret) {
      if(!(vec instanceof SparseNumberVector)) {
        return projectDense(vec, ret);
      }
      SparseNumberVector in = (SparseNumberVector) vec;
      final int k = this.k;
      for(int iter = in.iter(); in.iterValid(iter); iter = in.iterAdvance(iter)) {
        final int i = in.iterDim(iter);
        final double x = in.iterDoubleValue(iter);
        boolean[] row = mat[i]; // Rows and output are aligned.
        for(int o = 0; o < k; o++) {
          if(row[o]) {
            ret[o] += x;
          }
          else {
            ret[o] -= x;
          }
        }
      }
      return ret;
    }

    /**
     * Slower version, for dense multiplication.
     *
     * @param in Input vector
     * @return Projected data.
     */
    private double[] projectDense(NumberVector in, double[] ret) {
      final int k = this.k;
      final double dim = Math.min(buf.length, in.getDimensionality());
      for(int i = 0; i < dim; i++) {
        final boolean[] row = mat[i];
        double vali = in.doubleValue(i);
        for(int o = 0; o < k; o++) {
          if(row[o]) {
            ret[o] += vali;
          }
          else {
            ret[o] -= vali;
          }
        }
      }
      return ret;
    }

    @Override
    public int getOutputDimensionality() {
      return k;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Evgeniy Faerman
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected SimplifiedRandomHyperplaneProjectionFamily makeInstance() {
      return new SimplifiedRandomHyperplaneProjectionFamily(random);
    }
  }
}
