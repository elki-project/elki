package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta.BestFitEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Class to perform and undo a normalization on real vectors by estimating the
 * distribution of values along each dimension independently, then rescaling
 * objects to the cumulative density function (CDF) value at the original
 * coordinate.
 * 
 * This process is for example also discussed in section 3.4 of
 * <p>
 * Effects of Feature Normalization on Image Retrieval <br/>
 * S. Aksoy, R. M. Haralick
 * </p>
 * but they do not detail how to obtain an appropriate function `F`.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * @param <V> vector type
 * 
 * @apiviz.uses NumberVector
 * @apiviz.uses DistributionEstimator
 */
// TODO: extract superclass AbstractAttributeWiseNormalization
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseCDFNormalization"})
public class AttributeWiseCDFNormalization<V extends NumberVector> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseCDFNormalization.class);

  /**
   * Stores the distribution estimators
   */
  private List<DistributionEstimator<?>> estimators;

  /**
   * Stores the estimated distributions
   */
  private List<Distribution> dists;

  /**
   * Number vector factory.
   */
  protected NumberVector.Factory<V> factory;

  /**
   * Constructor.
   * 
   * @param estimators Distribution estimators
   */
  public AttributeWiseCDFNormalization(List<DistributionEstimator<?>> estimators) {
    super();
    this.estimators = estimators;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = (SimpleTypeInformation<?>) objects.meta(r);
      final List<?> column = (List<?>) objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      final List<V> castColumn = (List<V>) column;
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final VectorFieldTypeInformation<V> castType = (VectorFieldTypeInformation<V>) type;
      factory = FilterUtil.guessFactory(castType);

      // Scan to find the best
      final int dim = castType.getDimensionality();
      dists = new ArrayList<>(dim);
      // Scratch space for testing:
      double[] test = estimators.size() > 1 ? new double[castColumn.size()] : null;

      // We iterate over dimensions, this kind of filter needs fast random
      // access.
      Adapter adapter = new Adapter();
      for(int d = 0; d < dim; d++) {
        adapter.dim = d;
        Distribution dist;
        if(estimators.size() == 1) {
          dist = estimators.get(0).estimate(castColumn, adapter);
        }
        else {
          dist = findBestFit(castColumn, adapter, d, test);
        }
        // Special handling for constant distributions:
        // We want them to remain 0, instead of - usually - becoming constant .5
        if(dist instanceof UniformDistribution) {
          dist = constantZero(castColumn, adapter) ? new UniformDistribution(0., 1.) : dist;
        }
        dists.add(dist);
      }

      // Normalization scan
      double[] buf = new double[dim];
      for(int i = 0; i < objects.dataLength(); i++) {
        final V obj = castColumn.get(i);
        for(int d = 0; d < dim; d++) {
          buf[d] = dists.get(d).cdf(obj.doubleValue(d));
        }
        castColumn.set(i, factory.newNumberVector(buf));
      }
    }
    return objects;
  }

  /**
   * Find the best fitting distribution.
   * 
   * @param col Column of table
   * @param adapter Adapter for accessing the data
   * @param d Dimension
   * @param test Scatch space for testing goodness of fit
   * @return Best fit distribution
   */
  protected Distribution findBestFit(final List<V> col, Adapter adapter, int d, double[] test) {
    Distribution best = null;
    double bestq = Double.POSITIVE_INFINITY;
    trials: for(DistributionEstimator<?> est : estimators) {
      try {
        Distribution dist = est.estimate(col, adapter);
        for(int i = 0; i < test.length; i++) {
          test[i] = dist.cdf(col.get(i).doubleValue(d));
          if(Double.isNaN(test[i])) {
            LOG.warning("Got NaN after fitting " + est.toString() + ": " + dist.toString());
            continue trials;
          }
          if(Double.isInfinite(test[i])) {
            LOG.warning("Got infinite value after fitting " + est.toString() + ": " + dist.toString());
            continue trials;
          }
        }
        Arrays.sort(test);
        double q = KolmogorovSmirnovTest.simpleTest(test);
        if(LOG.isVeryVerbose()) {
          LOG.veryverbose("Estimator " + est.toString() + " (" + dist.toString() + ") has maximum deviation " + q + " for dimension " + d);
        }
        if(best == null || q < bestq) {
          best = dist;
          bestq = q;
        }
      }
      catch(ArithmeticException e) {
        if(LOG.isVeryVerbose()) {
          LOG.veryverbose("Fitting distribution " + est + " failed: " + e.getMessage());
        }
        continue trials;
      }
    }
    if(LOG.isVerbose()) {
      LOG.verbose("Best fit for dimension " + d + ": " + best.toString());
    }
    return best;
  }

  /**
   * Test if an attribute is constant zero.
   * 
   * @param column Column
   * @param adapter Data accessor.
   * @return {@code true} if all values are zero
   */
  protected boolean constantZero(List<V> column, Adapter adapter) {
    for(int i = 0, s = adapter.size(column); i < s; i++) {
      if(adapter.get(column, i) != 0.) {
        return false;
      }
    }
    return true;
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("normalization class: ").append(getClass().getName());
    result.append('\n');
    result.append("normalization distributions: ");
    boolean first = true;
    for(DistributionEstimator<?> est : estimators) {
      if(!first) {
        result.append(',');
      }
      first = false;
      result.append(est.getClass().getSimpleName());
    }
    return result.toString();
  }

  /**
   * Array adapter class for vectors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static class Adapter implements NumberArrayAdapter<Double, List<? extends NumberVector>> {
    /**
     * Dimension to process.
     */
    int dim;

    @Override
    public int size(List<? extends NumberVector> array) {
      return array.size();
    }

    @Override
    public Double get(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return getDouble(array, off);
    }

    @Override
    public double getDouble(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).doubleValue(dim);
    }

    @Override
    public float getFloat(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).floatValue(dim);
    }

    @Override
    public int getInteger(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).intValue(dim);
    }

    @Override
    public short getShort(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).shortValue(dim);
    }

    @Override
    public long getLong(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).longValue(dim);
    }

    @Override
    public byte getByte(List<? extends NumberVector> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).byteValue(dim);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for distribution estimators.
     */
    public static final OptionID DISTRIBUTIONS_ID = new OptionID("normalize.distributions", "A list of the distribution estimators to try.");

    /**
     * Stores the distribution estimators
     */
    private List<DistributionEstimator<?>> estimators;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectListParameter<DistributionEstimator<?>> estP = new ObjectListParameter<>(DISTRIBUTIONS_ID, DistributionEstimator.class);
      List<Class<? extends DistributionEstimator<?>>> def = new ArrayList<>(1);
      def.add(BestFitEstimator.class);
      estP.setDefaultValue(def);
      if(config.grab(estP)) {
        estimators = estP.instantiateClasses(config);
      }
    }

    @Override
    protected AttributeWiseCDFNormalization<V> makeInstance() {
      return new AttributeWiseCDFNormalization<>(estimators);
    }
  }
}
