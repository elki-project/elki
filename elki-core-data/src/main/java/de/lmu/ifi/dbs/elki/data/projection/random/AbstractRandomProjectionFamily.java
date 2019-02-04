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
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Abstract base class for random projection families.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public abstract class AbstractRandomProjectionFamily implements RandomProjectionFamily {
  /**
   * Random generator.
   */
  protected Random random;

  /**
   * Constructor.
   */
  public AbstractRandomProjectionFamily(RandomFactory random) {
    super();
    this.random = random.getSingleThreadedRandom();
  }

  /**
   * Class to project using a matrix multiplication. This class is optimized for
   * dense vector multiplications. In other words, the row dimensionality is the
   * output dimensionality, the column dimensionality is the input
   * dimensionality.
   *
   * It is <b>not thread safe</b> because it uses an internal buffer to store a
   * local copy of the vector.
   *
   * @author Erich Schubert
   */
  public static class MatrixProjection implements Projection {
    /**
     * Projection matrix.
     */
    double[][] matrix;

    /**
     * Shared buffer to use during projections.
     */
    private double[] buf;

    /**
     * Constructor.
     *
     * @param matrix Projection matrix ([output dim][input dim]).
     */
    public MatrixProjection(double[][] matrix) {
      super();
      this.matrix = matrix;
      this.buf = new double[matrix.length];
    }

    @Override
    public double[] project(NumberVector in) {
      return project(in, new double[matrix.length]);
    }

    @Override
    public double[] project(NumberVector in, double[] ret) {
      if(in instanceof SparseNumberVector) {
        return projectSparse((SparseNumberVector) in, ret);
      }
      final int dim = Math.min(buf.length, in.getDimensionality());
      assert (ret.length >= matrix.length) : "Output buffer too small!";
      // Copy vector into local buffer
      for(int i = 0; i < dim; i++) {
        buf[i] = in.doubleValue(i);
      }
      // Iterate over output dimensions:
      for(int o = 0; o < matrix.length; o++) {
        final double[] row = matrix[o];
        double v = 0.;
        for(int i = 0; i < dim; i++) {
          v += row[i] * buf[i]; // Rows and input are aligned.
        }
        ret[o] = v;
      }
      // Fill excess dimensions.
      for(int d = matrix.length; d < ret.length; d++) {
        ret[d] = 0;
      }
      return ret;
    }

    /**
     * Project, exploiting sparsity; but the transposed matrix layout would have
     * been better. For projections where you expect sparse input, consider the
     * opposite.
     *
     * @param in Input vector
     * @param ret Projection buffer
     * @return Projected data.
     */
    private double[] projectSparse(SparseNumberVector in, double[] ret) {
      Arrays.fill(ret, 0);
      for(int iter = in.iter(); in.iterValid(iter); iter = in.iterAdvance(iter)) {
        final int i = in.iterDim(iter);
        final double val = in.iterDoubleValue(iter);
        for(int o = 0; o < ret.length; o++) {
          ret[o] += val * matrix[o][i]; // Not aligned.
        }
      }
      return ret;
    }

    @Override
    public int getOutputDimensionality() {
      return matrix.length;
    }
  }

  /**
   * Parameterization interface (with the shared parameters)
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("randomproj.random", "Random generator seed.");

    /**
     * Random generator.
     */
    protected RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        random = rndP.getValue();
      }
    }
  }
}
