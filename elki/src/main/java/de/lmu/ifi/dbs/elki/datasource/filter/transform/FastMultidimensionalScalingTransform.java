package de.lmu.ifi.dbs.elki.datasource.filter.transform;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Rescale the data set using multidimensional scaling, MDS.
 *
 * This implementation uses power iterations, which is faster when the number of
 * data points is much larger than the desired number of dimensions.
 * 
 * This implementation is O(n^2), and uses O(n^2) memory.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Data type
 */
@Alias({ "fastmds" })
public class FastMultidimensionalScalingTransform<O> implements ObjectFilter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastMultidimensionalScalingTransform.class);

  /**
   * Distance function to use.
   */
  PrimitiveDistanceFunction<? super O> dist = null;

  /**
   * Target dimensionality
   */
  int tdim;

  /**
   * Constructor.
   * 
   * @param tdim Target dimensionality.
   * @param dist Distance function to use.
   */
  public FastMultidimensionalScalingTransform(int tdim, PrimitiveDistanceFunction<? super O> dist) {
    super();
    this.tdim = tdim;
    this.dist = dist;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    final int size = objects.dataLength();
    if(size == 0) {
      return objects;
    }
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    for(int r = 0; r < objects.metaLength(); r++) {
      @SuppressWarnings("unchecked")
      SimpleTypeInformation<Object> type = (SimpleTypeInformation<Object>) objects.meta(r);
      @SuppressWarnings("unchecked")
      final List<Object> column = (List<Object>) objects.getColumn(r);
      if(!dist.getInputTypeRestriction().isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final List<O> castColumn = (List<O>) column;
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
      double[][] imat = computeDistanceMatrix(castColumn, size);
      ClassicMultidimensionalScalingTransform.doubleCenterSymmetric(imat);
      // Find eigenvectors.
      {
        double[][] evs = new double[tdim][size];
        double[] lambda = new double[tdim];
        findEigenVectors(imat, size, evs, lambda);

        // Scaling factors.
        for(int d = 0; d < tdim; d++) {
          lambda[d] = Math.sqrt(lambda[d]);
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

  protected void findEigenVectors(double[][] imat, final int size, double[][] evs, double[] lambda) {
    double[] tmp = new double[size];
    Random rnd = new Random(); // FIXME: make parameterizable
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Learning projections.", tdim, LOG) : null;
    for(int d = 0; d < tdim; d++) {
      double[] cur = evs[d];
      randomInitialization(cur, rnd);
      double l = 0.;
      for(int iter = 0; iter < 100; iter++) {
        l = multiply(imat, cur, tmp);
        double delta = updateEigenvector(tmp, cur, l);
        if(delta < 1e-14) {
          break;
        }
      }
      lambda[d] = l;
      if(d + 1 < tdim) {
        for(int i = 0; i < size; i++) {
          for(int j = 0; j < size; j++) {
            imat[i][j] -= l * cur[i] * cur[j];
          }
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  protected double[][] computeDistanceMatrix(final List<O> castColumn, final int size) {
    double[][] imat = new double[size][size];
    boolean squared = dist instanceof SquaredEuclideanDistanceFunction;
    FiniteProgress dprog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix.", (size * (size - 1)) >>> 1, LOG) : null;
    for(int x = 0; x < size; x++) {
      final O ox = castColumn.get(x);
      for(int y = x + 1; y < size; y++) {
        final O oy = castColumn.get(y);
        double distance = dist.distance(ox, oy);
        distance *= (squared ? -.5 : -.5 * distance);
        imat[x][y] = distance;
        imat[y][x] = distance;
        LOG.incrementProcessed(dprog);
      }
    }
    LOG.ensureCompleted(dprog);
    return imat;
  }

  protected void randomInitialization(double[] out, Random rnd) {
    double l2 = 0.;
    while(!(l2 > 0)) {
      for(int d = 0; d < out.length; d++) {
        double val = rnd.nextDouble();
        out[d] = val;
        l2 += val * val;
      }
    }
    // Standardize:
    if(l2 > 0) {
      double s = 1. / Math.sqrt(l2);
      for(int d = 0; d < out.length; d++) {
        out[d] *= s;
      }
    }
  }

  private double multiply(double[][] mat, double[] in, double[] out) {
    double l2 = 0.;
    // Matrix multiplication:
    for(int d1 = 0; d1 < in.length; d1++) {
      double[] row = mat[d1];
      double t = 0.;
      for(int d2 = 0; d2 < in.length; d2++) {
        t += row[d2] * in[d2];
      }
      out[d1] = t;
      l2 += t * t;
    }
    return Math.sqrt(l2);
  }

  private double updateEigenvector(double[] in, double[] out, double l) {
    final double s = l > 0 ? 1. / l : 1;
    double diff = 0.;
    for(int d = 0; d < in.length; d++) {
      // Scale to unit length
      in[d] *= s;
      // Compute change from previous iteration:
      double delta = in[d] - out[d];
      diff += delta * delta;
      // Update.
      out[d] = in[d];
    }
    return diff;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Target dimensionality.
     */
    int tdim;

    /**
     * Distance function to use.
     */
    PrimitiveDistanceFunction<? super O> dist = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter dimP = new IntParameter(ClassicMultidimensionalScalingTransform.Parameterizer.DIM_ID);
      if(config.grab(dimP)) {
        tdim = dimP.intValue();
      }

      ObjectParameter<PrimitiveDistanceFunction<? super O>> distP = new ObjectParameter<>(ClassicMultidimensionalScalingTransform.Parameterizer.DISTANCE_ID, PrimitiveDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        dist = distP.instantiateClass(config);
      }
    }

    @Override
    protected FastMultidimensionalScalingTransform<O> makeInstance() {
      return new FastMultidimensionalScalingTransform<>(tdim, dist);
    }
  }
}
