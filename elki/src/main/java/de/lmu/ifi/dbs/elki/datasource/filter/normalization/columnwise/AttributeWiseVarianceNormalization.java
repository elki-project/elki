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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractNormalization;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Class to perform and undo a normalization on real vectors with respect to
 * given mean and standard deviation in each dimension.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * @param <V> vector type
 * 
 * @apiviz.uses NumberVector
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseVarianceNormalization", //
"z", "de.lmu.ifi.dbs.elki.datasource.filter.AttributeWiseVarianceNormalization" })
public class AttributeWiseVarianceNormalization<V extends NumberVector> extends AbstractNormalization<V> {
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
   * 
   * @param mean Mean value
   * @param stddev Standard deviation
   */
  public AttributeWiseVarianceNormalization(double[] mean, double[] stddev) {
    super();
    this.mean = mean;
    this.stddev = stddev;
  }

  /**
   * Constructor.
   */
  public AttributeWiseVarianceNormalization() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    return (mean == null || stddev == null || mean.length == 0 || stddev.length == 0);
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    // First object? Then init. (We didn't have a dimensionality before!)
    if(mvs == null || mvs.length == 0) {
      int dimensionality = featureVector.getDimensionality();
      mvs = MeanVariance.newArray(dimensionality);
    }
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      mvs[d].put(featureVector.doubleValue(d));
    }
  }

  @Override
  protected void prepareComplete() {
    StringBuilder buf = LOG.isVerbose() ? new StringBuilder() : null;
    final int dimensionality = mvs.length;
    mean = new double[dimensionality];
    stddev = new double[dimensionality];
    if(buf != null) {
      buf.append("Normalization parameters: ");
    }
    for(int d = 0; d < dimensionality; d++) {
      mean[d] = mvs[d].getMean();
      stddev[d] = mvs[d].getSampleStddev();
      if(stddev[d] == 0 || Double.isNaN(stddev[d])) {
        stddev[d] = 1.0;
      }
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

    for(int i = 0; i < coeff.length; i++) {
      for(int r = 0; r < coeff.length; r++) {
        double sum = 0.0;
        for(int c = 0; c < coeff[0].length; c++) {
          sum += mean[c] * coeff[row[r]][col[c]] / stddev[c];
          coeff[row[r]][col[c]] = coeff[row[r]][col[c]] / stddev[c];
        }
        rhs[row[r]] = rhs[row[r]] + sum;
      }
    }

    return new LinearEquationSystem(coeff, rhs, row, col);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("normalization class: ").append(getClass().getName());
    result.append('\n');
    result.append("normalization means: ").append(FormatUtil.format(mean));
    result.append('\n');
    result.append("normalization stddevs: ").append(FormatUtil.format(stddev));

    return result.toString();
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
   * 
   * @apiviz.exclude
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
      .setOptional(true);
      if(config.grab(stddevP)) {
        stddev = stddevP.getValue().clone();

        for(double d : stddev) {
          if(d == 0.) {
            config.reportError(new WrongParameterValueException("Standard deviations must not be 0."));
          }
        }
      }
      config.checkConstraint(new AllOrNoneMustBeSetGlobalConstraint(meanP, stddevP));
      config.checkConstraint(new EqualSizeGlobalConstraint(meanP, stddevP));
    }

    @Override
    protected AttributeWiseVarianceNormalization<V> makeInstance() {
      return new AttributeWiseVarianceNormalization<>(mean, stddev);
    }
  }
}
