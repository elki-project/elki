package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random hyperplane projection family.
 *
 * Reference:
 * <p>
 * M. Henzinger<br />
 * Finding near-duplicate web pages: a large-scale evaluation of algorithms
 * <br />
 * Proc. 29th ACM Conference on Research and Development in Information
 * Retrieval. ACM SIGIR, 2006
 * </p>
 *
 * @author Evgeniy Faerman
 * @author Erich Schubert
 */
@Reference(authors = "M. Henzinger", //
title = "Finding near-duplicate web pages: a large-scale evaluation of algorithms", //
booktitle = "Proc. 29th ACM Conference on Research and Development in Information Retrieval. ACM SIGIR, 2006", //
url = "http://dx.doi.org/10.1145/1148170.1148222")
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
    }

    @Override
    public double[] project(NumberVector in) {
      if(in instanceof SparseNumberVector) {
        return projectSparse((SparseNumberVector) in);
      }
      final int k = this.k;
      final double d = in.getDimensionality();
      double[] ret = new double[k];
      for(int i = 0; i < d; i++) {
        double val = in.doubleValue(i);
        boolean[] row = mat[i];
        for(int j = 0; j < k; j++) {
          if(row[j]) {
            ret[j] += val;
          }
          else {
            ret[j] -= val;
          }
        }
      }
      return ret;
    }

    /**
     * Project, exploiting sparsity.
     *
     * @param in Input vector
     * @return Projected data.
     */
    private double[] projectSparse(SparseNumberVector in) {
      final int k = this.k;
      double[] ret = new double[k];
      for(int iter = in.iter(); in.iterValid(iter); iter = in.iterAdvance(iter)) {
        final int dim = in.iterDim(iter);
        final double val = in.iterDoubleValue(iter);
        boolean[] row = mat[dim];
        for(int j = 0; j < k; j++) {
          if(row[j]) {
            ret[j] += val;
          }
          else {
            ret[j] -= val;
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected SimplifiedRandomHyperplaneProjectionFamily makeInstance() {
      return new SimplifiedRandomHyperplaneProjectionFamily(random);
    }
  }
}
