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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.BetaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta.BestFitEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Project the data using a Beta distribution.
 * 
 * This is a crude heuristic, that may or may not work for your data set. There
 * currently is no theoretical foundation of why it may be sensible or not to do
 * this.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <V> vector type
 * 
 * @apiviz.uses NumberVector
 * @apiviz.uses DistributionEstimator
 */
public class AttributeWiseBetaNormalization<V extends NumberVector> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseBetaNormalization.class);

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
   * Expected outlier rate alpha.
   */
  protected double alpha = 0.01;

  /**
   * Constructor.
   * 
   * @param estimators Distribution estimators
   */
  public AttributeWiseBetaNormalization(List<DistributionEstimator<?>> estimators, double alpha) {
    super();
    this.estimators = estimators;
    this.alpha = alpha;
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
      double[] test = new double[castColumn.size()];

      // We iterate over dimensions, this kind of filter needs fast random
      // access.
      Adapter adapter = new Adapter();
      for(int d = 0; d < dim; d++) {
        adapter.dim = d;
        if(estimators.size() == 1) {
          dists.add(estimators.get(0).estimate(castColumn, adapter));
          continue;
        }
        Distribution best = null;
        double bestq = Double.POSITIVE_INFINITY;
        trials: for(DistributionEstimator<?> est : estimators) {
          try {
            Distribution dist = est.estimate(castColumn, adapter);
            for(int i = 0; i < test.length; i++) {
              test[i] = dist.cdf(castColumn.get(i).doubleValue(d));
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
            continue;
          }
        }
        if(LOG.isVerbose()) {
          LOG.verbose("Best fit for dimension " + d + ": " + best.toString());
        }
        dists.add(best);
      }

      // Beta distribution for projection
      double p = Math.pow(alpha, -1 / Math.sqrt(dim));
      BetaDistribution beta = new BetaDistribution(p, p);
      // Normalization scan
      double[] buf = new double[dim];
      for(int i = 0; i < objects.dataLength(); i++) {
        final V obj = castColumn.get(i);
        for(int d = 0; d < dim; d++) {
          // TODO: when available, use logspace for better numerical precision!
          buf[d] = beta.quantile(dists.get(d).cdf(obj.doubleValue(d)));
        }
        castColumn.set(i, factory.newNumberVector(buf));
      }
    }
    return objects;
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
    public static final OptionID DISTRIBUTIONS_ID = new OptionID("betanormalize.distributions", "A list of the distribution estimators to try.");

    /**
     * Shape parameter.
     */
    public static final OptionID ALPHA_ID = new OptionID("betanormalize.alpha", "Alpha parameter to control the shape of the output distribution.");

    /**
     * Stores the distribution estimators
     */
    private List<DistributionEstimator<?>> estimators;

    /**
     * Expected outlier rate alpha.
     */
    private double alpha;

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

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.1);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected AttributeWiseBetaNormalization<V> makeInstance() {
      return new AttributeWiseBetaNormalization<>(estimators, alpha);
    }
  }
}
