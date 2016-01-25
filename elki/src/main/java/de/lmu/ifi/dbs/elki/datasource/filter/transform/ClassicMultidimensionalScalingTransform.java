package de.lmu.ifi.dbs.elki.datasource.filter.transform;

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

import java.util.List;

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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SingularValueDecomposition;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Rescale the data set using multidimensional scaling, MDS.
 * 
 * Note: the current implementation is rather expensive, both memory- and
 * runtime wise. Don't use for large data sets! Instead, have a look at
 * {@link FastMultidimensionalScalingTransform} which uses power iterations
 * instead.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.composedOf SingularValueDecomposition
 * 
 * @param <O> Data type
 */
@Alias({ "mds" })
public class ClassicMultidimensionalScalingTransform<O> implements ObjectFilter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClassicMultidimensionalScalingTransform.class);

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
  public ClassicMultidimensionalScalingTransform(int tdim, PrimitiveDistanceFunction<? super O> dist) {
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
      Matrix mat = new Matrix(computeDistanceMatrix(castColumn, size));
      doubleCenterSymmetric(mat.getArrayRef());
      // Find eigenvectors.
      {
        SingularValueDecomposition svd = new SingularValueDecomposition(mat);
        Matrix u = svd.getU();
        double[] lambda = svd.getSingularValues();
        for(int i = 0; i < tdim; i++) {
          lambda[i] = Math.sqrt(Math.abs(lambda[i]));
        }

        double[] buf = new double[tdim];
        double[][] uraw = u.getArrayRef();
        for(int i = 0; i < size; i++) {
          double[] raw = uraw[i];
          for(int x = 0; x < buf.length; x++) {
            buf[x] = lambda[x] * raw[x];
          }
          column.set(i, factory.newNumberVector(buf));
        }
      }
    }
    return bundle;
  }

  protected double[][] computeDistanceMatrix(final List<O> castColumn, final int size) {
    double[][] imat = new double[size][size];
    boolean squared = dist instanceof SquaredEuclideanDistanceFunction;
    FiniteProgress dprog = LOG.isVerbose() ? new FiniteProgress("Computing distance matrix", (size * (size - 1)) >>> 1, LOG) : null;
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

  /**
   * Double-center the given matrix (only upper triangle is used).
   * 
   * For improved numerical precision, we perform incremental updates to the
   * mean values, instead of computing a large sum and then performing division.
   * 
   * @param m Matrix to double-center.
   */
  public static void doubleCenterSymmetric(double[][] m) {
    final int size = m.length;
    // Storage for mean values - initially all 0.
    double means[] = new double[size];
    for(int x = 0; x < m.length; x++) {
      final double[] rowx = m[x];
      // We already added "x" values in previous iterations.
      // Fake-add 0: mean + (0 - mean) / (x + 1)
      double rmean = means[x] - means[x] / (x + 1);
      for(int y = x + 1; y < rowx.length; y++) {
        final double nv = rowx[y];
        final double dx = nv - rmean, dy = nv - means[y];
        // For x < y, this is the yth entry.
        rmean += dx / (y + 1);
        // For y > x, this is the xth entry
        means[y] += dy / (x + 1);
      }
      means[x] = rmean;
    }
    // Compute total mean by averaging column means.
    double mean = means[0];
    for(int x = 1; x < size; x++) {
      double dm = means[x] - mean;
      mean += dm / (x + 1);
    }
    // Row and column center; also make symmetric.
    for(int x = 0; x < size; x++) {
      m[x][x] = -2. * means[x] + mean;
      for(int y = x + 1; y < size; y++) {
        final double nv = m[x][y] - means[x] - means[y] + mean;
        m[x][y] = nv;
        m[y][x] = nv;
      }
    }
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
     * Desired dimensionality.
     */
    public static final OptionID DIM_ID = new OptionID("mds.dim", "Output dimensionality.");

    /**
     * Distant metric.
     */
    public static final OptionID DISTANCE_ID = new OptionID("mds.distance", "Distance function to use.");

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

      IntParameter dimP = new IntParameter(DIM_ID);
      if(config.grab(dimP)) {
        tdim = dimP.intValue();
      }

      ObjectParameter<PrimitiveDistanceFunction<? super O>> distP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        dist = distP.instantiateClass(config);
      }
    }

    @Override
    protected ClassicMultidimensionalScalingTransform<O> makeInstance() {
      return new ClassicMultidimensionalScalingTransform<>(tdim, dist);
    }
  }
}
