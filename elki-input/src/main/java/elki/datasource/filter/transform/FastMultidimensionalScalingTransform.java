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
package elki.datasource.filter.transform;

import static elki.datasource.filter.transform.ClassicMultidimensionalScalingTransform.computeSquaredDistanceMatrix;
import static elki.datasource.filter.transform.ClassicMultidimensionalScalingTransform.doubleCenterSymmetric;

import java.util.List;
import java.util.Random;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.FilterUtil;
import elki.datasource.filter.ObjectFilter;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Rescale the data set using multidimensional scaling, MDS.
 * <p>
 * This implementation uses power iterations, which is faster when the number of
 * data points is much larger than the desired number of dimensions.
 * <p>
 * This implementation is O(n²), and uses O(n²) memory.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <I> Data type
 */
@Alias({ "fastmds" })
@Priority(Priority.IMPORTANT - 1)
public class FastMultidimensionalScalingTransform<I, O extends NumberVector> implements ObjectFilter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastMultidimensionalScalingTransform.class);

  /**
   * Distance function to use.
   */
  PrimitiveDistance<? super I> dist;

  /**
   * Target dimensionality
   */
  int tdim;

  /**
   * Random generator.
   */
  RandomFactory random;

  /**
   * Vector factory.
   */
  NumberVector.Factory<O> factory;

  /**
   * Constructor.
   *
   * @param tdim Target dimensionality.
   * @param dist Distance function to use.
   * @param factory Vector factory.
   * @param random Random generator.
   */
  public FastMultidimensionalScalingTransform(int tdim, PrimitiveDistance<? super I> dist, NumberVector.Factory<O> factory, RandomFactory random) {
    super();
    this.tdim = tdim;
    this.dist = dist;
    this.random = random;
    this.factory = factory;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    final int size = objects.dataLength();
    if(size == 0) {
      return objects;
    }
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<? extends Object> type = objects.meta(r);
      @SuppressWarnings("unchecked")
      final List<Object> column = (List<Object>) objects.getColumn(r);
      // Not supported column (e.g. labels):
      if(!dist.getInputTypeRestriction().isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final List<I> castColumn = (List<I>) column;
      NumberVector.Factory<? extends NumberVector> factory = null;
      {
        if(type instanceof VectorFieldTypeInformation) {
          final VectorFieldTypeInformation<?> ctype = (VectorFieldTypeInformation<?>) type;
          // Note two-step cast, to make stricter compilers happy.
          @SuppressWarnings("unchecked")
          final VectorFieldTypeInformation<? extends NumberVector> vtype = (VectorFieldTypeInformation<? extends NumberVector>) ctype;
          factory = FilterUtil.guessFactory(vtype);
        }
        else {
          factory = DoubleVector.FACTORY;
        }
        bundle.appendColumn(new VectorFieldTypeInformation<>(factory, tdim), castColumn);
      }

      // Compute distance matrix.
      double[][] imat = computeSquaredDistanceMatrix(castColumn, dist);
      doubleCenterSymmetric(imat);
      // Find eigenvectors.
      {
        double[][] evs = new double[tdim][size];
        double[] lambda = new double[tdim];
        findEigenVectors(imat, evs, lambda);
        // Undo squared, unless we were given a squared distance function:
        if(!dist.isSquared()) {
          for(int i = 0; i < tdim; i++) {
            lambda[i] = Math.sqrt(Math.abs(lambda[i]));
          }
        }

        // Project each data point to the new coordinates.
        double[] buf = new double[tdim];
        for(int i = 0; i < size; i++) {
          for(int d = 0; d < tdim; d++) {
            buf[d] = lambda[d] * evs[d][i];
          }
          column.set(i, factory.newNumberVector(buf));
        }
      }
    }
    return bundle;
  }

  /**
   * Find the first eigenvectors and eigenvalues using power iterations.
   *
   * @param imat Matrix (will be modified!)
   * @param evs Eigenvectors output
   * @param lambda Eigenvalues output
   */
  protected void findEigenVectors(double[][] imat, double[][] evs, double[] lambda) {
    final int size = imat.length;
    Random rnd = random.getSingleThreadedRandom();
    double[] tmp = new double[size];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Learning projections", tdim, LOG) : null;
    for(int d = 0; d < tdim;) {
      final double[] cur = evs[d];
      randomInitialization(cur, rnd);
      double l = multiply(imat, cur, tmp);
      for(int iter = 0; iter < 100; iter++) {
        // This will scale "tmp" to unit length, and copy it to cur:
        double delta = updateEigenvector(tmp, cur, l);
        if(delta < 1e-10) {
          break;
        }
        l = multiply(imat, cur, tmp);
      }
      lambda[d++] = l = estimateEigenvalue(imat, cur);
      LOG.incrementProcessed(prog);
      if(d == tdim) {
        break;
      }
      // Update matrix
      updateMatrix(imat, cur, l);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Choose a random vector of unit norm for power iterations.
   *
   * @param out Output storage
   * @param rnd Random source.
   */
  protected void randomInitialization(double[] out, Random rnd) {
    double l2 = 0.;
    while(!(l2 > 0)) {
      for(int d = 0; d < out.length; d++) {
        final double val = rnd.nextDouble();
        out[d] = val;
        l2 += val * val;
      }
    }
    // Standardize:
    final double s = 1. / Math.sqrt(l2);
    for(int d = 0; d < out.length; d++) {
      out[d] *= s;
    }
  }

  /**
   * Compute out = A * in, and return the (signed!) length of the output vector.
   *
   * @param mat Matrix.
   * @param in Input vector.
   * @param out Output vector storage.
   * @return Length of this vector.
   */
  protected double multiply(double[][] mat, double[] in, double[] out) {
    double l = 0.;
    // Matrix multiplication:
    for(int d1 = 0; d1 < in.length; d1++) {
      final double[] row = mat[d1];
      double t = 0.;
      for(int d2 = 0; d2 < in.length; d2++) {
        t += row[d2] * in[d2];
      }
      out[d1] = t;
      l += t * t;
    }
    return l > 0 ? Math.sqrt(l) : 0.;
  }

  /**
   * Compute the change in the eigenvector, and normalize the output vector
   * while doing so.
   *
   * @param in Input vector
   * @param out Output vector
   * @param l Eigenvalue
   * @return Change
   */
  protected double updateEigenvector(double[] in, double[] out, double l) {
    double s = 1. / (l > 0. ? l : l < 0. ? -l : 1.);
    s = (in[0] > 0.) ? s : -s; // Reduce flipping vectors
    double diff = 0.;
    for(int d = 0; d < in.length; d++) {
      in[d] *= s; // Scale to unit length
      // Compute change from previous iteration:
      double delta = in[d] - out[d];
      diff += delta * delta;
      out[d] = in[d]; // Update output storage
    }
    return diff;
  }

  /**
   * Estimate the (singed!) Eigenvalue for a particular vector.
   *
   * @param mat Matrix.
   * @param in Input vector.
   * @return Estimated eigenvalue
   */
  protected double estimateEigenvalue(double[][] mat, double[] in) {
    double de = 0., di = 0.;
    // Matrix multiplication:
    for(int d1 = 0; d1 < in.length; d1++) {
      final double[] row = mat[d1];
      double t = 0.;
      for(int d2 = 0; d2 < in.length; d2++) {
        t += row[d2] * in[d2];
      }
      final double s = in[d1];
      de += t * s;
      di += s * s;
    }
    return de / di;
  }

  /**
   * Update matrix, by removing the effects of a known Eigenvector.
   *
   * @param mat Matrix
   * @param evec Known normalized Eigenvector
   * @param eval Eigenvalue
   */
  protected void updateMatrix(double[][] mat, final double[] evec, double eval) {
    final int size = mat.length;
    for(int i = 0; i < size; i++) {
      final double[] mati = mat[i];
      final double eveci = evec[i];
      for(int j = 0; j < size; j++) {
        mati[j] -= eval * eveci * evec[j];
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<I, O extends NumberVector> implements Parameterizer {
    /**
     * Random seed generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("mds.seed", "Random seed for fast MDS.");

    /**
     * Target dimensionality.
     */
    int tdim;

    /**
     * Distance function to use.
     */
    PrimitiveDistance<? super I> dist = null;

    /**
     * Random generator.
     */
    RandomFactory random = RandomFactory.DEFAULT;

    /**
     * Vector factory.
     */
    NumberVector.Factory<O> factory;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(ClassicMultidimensionalScalingTransform.Par.DIM_ID) //
          .grab(config, x -> tdim = x);
      new ObjectParameter<PrimitiveDistance<? super I>>(ClassicMultidimensionalScalingTransform.Par.DISTANCE_ID, PrimitiveDistance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> dist = x);
      new RandomParameter(RANDOM_ID).grab(config, x -> random = x);
      new ObjectParameter<NumberVector.Factory<O>>(ClassicMultidimensionalScalingTransform.Par.VECTOR_TYPE_ID, NumberVector.Factory.class, DoubleVector.Factory.class) //
          .grab(config, x -> factory = x);
    }

    @Override
    public FastMultidimensionalScalingTransform<I, O> make() {
      return new FastMultidimensionalScalingTransform<>(tdim, dist, factory, random);
    }
  }
}
