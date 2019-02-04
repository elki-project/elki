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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Class to perform and undo a normalization on real vectors with respect to
 * given mean and standard deviation in each dimension.
 *
 * We use the biased variance ({@link MeanVariance#getNaiveStddev()}), because
 * this produces that with exactly standard deviation 1. While often the
 * unbiased estimate ({@link MeanVariance#getSampleStddev()}) is more
 * appropriate, it will not ensure this interesting property. For large data,
 * the difference will be small anyway.
 *
 * @author Erich Schubert
 * @since 0.4.0
 * @param <V> vector type
 *
 * @assoc - - - NumberVector
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseVarianceNormalization", //
    "z", "de.lmu.ifi.dbs.elki.datasource.filter.AttributeWiseVarianceNormalization" })
@Priority(Priority.RECOMMENDED)
public class AttributeWiseVarianceNormalization<V extends NumberVector> extends AbstractVectorConversionFilter<V, V> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseVarianceNormalization.class);

  /**
   * Stores the mean in each dimension.
   */
  private double[] mean;

  /**
   * Stores the standard deviation in each dimension.
   */
  private double[] stddev;

  /**
   * Temporary storage used during initialization.
   */
  MeanVariance[] mvs = null;

  /**
   * Constructor.
   */
  public AttributeWiseVarianceNormalization() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param mean Mean value
   * @param stddev Standard deviation
   */
  public AttributeWiseVarianceNormalization(double[] mean, double[] stddev) {
    super();
    this.mean = mean;
    this.stddev = stddev;
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    return (mean == null || stddev == null || mean.length == 0 || stddev.length == 0);
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    // First object? Then init. (We didn't have a dimensionality before!)
    if(mvs == null || mvs.length == 0) {
      mvs = MeanVariance.newArray(featureVector.getDimensionality());
    }
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      final double v = featureVector.doubleValue(d);
      if(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) {
        mvs[d].put(v);
      }
    }
  }

  @Override
  protected void prepareComplete() {
    StringBuilder buf = LOG.isVerbose() ? new StringBuilder(300) : null;
    final int dimensionality = mvs.length;
    mean = new double[dimensionality];
    stddev = new double[dimensionality];
    if(buf != null) {
      buf.append("Normalization parameters: ");
    }
    for(int d = 0; d < dimensionality; d++) {
      mean[d] = mvs[d].getMean();
      stddev[d] = mvs[d].getNaiveStddev();
      stddev[d] = stddev[d] > Double.MIN_NORMAL ? stddev[d] : 1.;
      if(buf != null) {
        buf.append(" m: ").append(mean[d]).append(" v: ").append(stddev[d]);
      }
    }
    mvs = null;
    if(buf != null) {
      LOG.debugFine(buf.toString());
    }
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] values = new double[featureVector.getDimensionality()];
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      values[d] = normalize(d, featureVector.doubleValue(d));
    }
    return factory.newNumberVector(values);
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    if(featureVector.getDimensionality() != mean.length) {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: " + featureVector.getDimensionality() + " former dimensionality: " + mean.length);
    }
    double[] values = new double[featureVector.getDimensionality()];
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      values[d] = restore(d, featureVector.doubleValue(d));
    }
    return factory.newNumberVector(values);
  }

  /**
   * Normalize a single dimension.
   * 
   * @param d Dimension
   * @param val Value
   * @return Normalized value
   */
  private double normalize(int d, double val) {
    d = (mean.length == 1) ? 0 : d;
    return (val - mean[d]) / stddev[d];
  }

  /**
   * Restore a single dimension.
   * 
   * @param d Dimension
   * @param val Value
   * @return Normalized value
   */
  private double restore(int d, double val) {
    d = (mean.length == 1) ? 0 : d;
    return (val * stddev[d]) + mean[d];
  }

  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    double[][] coeff = linearEquationSystem.getCoefficents();
    double[] rhs = linearEquationSystem.getRHS();
    int[] row = linearEquationSystem.getRowPermutations();
    int[] col = linearEquationSystem.getColumnPermutations();

    for(int r = 0; r < coeff.length; r++) {
      final double[] coeff_r = coeff[row[r]];
      double sum = 0.0;
      for(int c = 0; c < coeff_r.length; c++) {
        sum += mean[c] * (coeff_r[col[c]] /= stddev[c]);
      }
      rhs[row[r]] += sum;
    }

    return new LinearEquationSystem(coeff, rhs, row, col);
  }

  @Override
  public String toString() {
    return new StringBuilder(200) //
        .append("normalization class: ").append(getClass().getName()).append('\n')//
        .append("normalization means: ").append(FormatUtil.format(mean)).append('\n')//
        .append("normalization stddevs: ").append(FormatUtil.format(stddev))//
        .toString();
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return in;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for means.
     */
    public static final OptionID MEAN_ID = new OptionID("normalize.mean", "a comma separated concatenation of the mean values in each dimension that are mapped to 0. If no value is specified, the mean value of the attribute range in this dimension will be taken.");

    /**
     * Parameter for stddevs.
     */
    public static final OptionID STDDEV_ID = new OptionID("normalize.stddev", "a comma separated concatenation of the standard deviations in each dimension that are scaled to 1. If no value is specified, the standard deviation of the attribute range in this dimension will be taken.");

    /**
     * Stores the mean in each dimension.
     */
    private double[] mean = new double[0];

    /**
     * Stores the standard deviation in each dimension.
     */
    private double[] stddev = new double[0];

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter meanP = new DoubleListParameter(MEAN_ID) //
          .setOptional(true);
      if(config.grab(meanP)) {
        mean = meanP.getValue().clone();
      }
      DoubleListParameter stddevP = new DoubleListParameter(STDDEV_ID) //
          .setOptional(!meanP.isDefined());
      if(config.grab(stddevP)) {
        stddev = stddevP.getValue().clone();

        for(double d : stddev) {
          if(d == 0.) {
            config.reportError(new WrongParameterValueException(stddevP, stddevP.getValueAsString(), "Standard deviations must not be 0."));
          }
        }
      }
      // Non-formalized parameter constraint:
      if(mean != null && stddev != null && mean.length != stddev.length) {
        config.reportError(new WrongParameterValueException(meanP, "and", stddevP, "must have the same number of values."));
      }
    }

    @Override
    protected AttributeWiseVarianceNormalization<V> makeInstance() {
      return new AttributeWiseVarianceNormalization<>(mean, stddev);
    }
  }
}
