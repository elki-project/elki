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
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.AllOrNoneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualSizeGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;

/**
 * Class to perform and undo a normalization on real vectors with respect to
 * given minimum and maximum in each dimension.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 * @param <V> vector type
 * 
 * @apiviz.uses NumberVector
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseMinMaxNormalization", //
"de.lmu.ifi.dbs.elki.datasource.filter.AttributeWiseMinMaxNormalization" })
public class AttributeWiseMinMaxNormalization<V extends NumberVector> extends AbstractNormalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseMinMaxNormalization.class);

  /**
   * Stores the maximum in each dimension.
   */
  private double[] maxima = new double[0];

  /**
   * Stores the minimum in each dimension.
   */
  private double[] minima = new double[0];

  /**
   * Constructor.
   * 
   * @param minima Minimum values
   * @param maxima Maximum values
   */
  public AttributeWiseMinMaxNormalization(double[] minima, double[] maxima) {
    super();
    this.minima = minima;
    this.maxima = maxima;
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    return (minima.length == 0 || maxima.length == 0);
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    // First object? Then initialize.
    if(minima.length == 0 || maxima.length == 0) {
      int dimensionality = featureVector.getDimensionality();
      minima = new double[dimensionality];
      maxima = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        maxima[i] = -Double.MAX_VALUE;
        minima[i] = Double.MAX_VALUE;
      }
    }
    if(minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      final double val = featureVector.doubleValue(d);
      if(val > maxima[d]) {
        maxima[d] = val;
      }
      if(val < minima[d]) {
        minima[d] = val;
      }
    }
  }

  @Override
  protected V filterSingleObject(V featureVector) {
    double[] values = new double[featureVector.getDimensionality()];
    if(minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors and given Minima/Maxima differ in length.");
    }
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      values[d] = (featureVector.doubleValue(d) - minima[d]) / factor(d);
    }
    return factory.newNumberVector(values);
  }

  @Override
  public V restore(V featureVector) throws NonNumericFeaturesException {
    if(featureVector.getDimensionality() != maxima.length || featureVector.getDimensionality() != minima.length) {
      throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: " + featureVector.getDimensionality() + " former dimensionality: " + maxima.length);
    }
    double[] values = new double[featureVector.getDimensionality()];
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      values[d] = featureVector.doubleValue(d) * factor(d) + minima[d];
    }
    return factory.newNumberVector(values);
  }

  /**
   * Returns a factor for normalization in a certain dimension.
   * <p/>
   * The provided factor is the maximum-minimum in the specified dimension, if
   * these two values differ, otherwise it is the maximum if this value differs
   * from 0, otherwise it is 1.
   * 
   * @param dimension the dimension to get a factor for normalization
   * @return a factor for normalization in a certain dimension
   */
  private double factor(int dimension) {
    return maxima[dimension] > minima[dimension] ? maxima[dimension] - minima[dimension] : maxima[dimension] > 0 ? maxima[dimension] : 1;
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
          sum += minima[c] * coeff[row[r]][col[c]] / factor(c);
          coeff[row[r]][col[c]] = coeff[row[r]][col[c]] / factor(c);
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
    result.append("normalization minima: ").append(FormatUtil.format(minima));
    result.append('\n');
    result.append("normalization maxima: ").append(FormatUtil.format(maxima));
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
     * Parameter for minimum.
     */
    public static final OptionID MINIMA_ID = new OptionID("normalize.min", "a comma separated concatenation of the minimum values in each dimension that are mapped to 0. If no value is specified, the minimum value of the attribute range in this dimension will be taken.");

    /**
     * Parameter for maximum.
     */
    public static final OptionID MAXIMA_ID = new OptionID("normalize.max", "a comma separated concatenation of the maximum values in each dimension that are mapped to 1. If no value is specified, the maximum value of the attribute range in this dimension will be taken.");

    /**
     * Stores the maximum in each dimension.
     */
    private double[] maxima = new double[0];

    /**
     * Stores the minimum in each dimension.
     */
    private double[] minima = new double[0];

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleListParameter minimaP = new DoubleListParameter(MINIMA_ID, true);
      if(config.grab(minimaP)) {
        minima = minimaP.getValue().clone();
      }
      DoubleListParameter maximaP = new DoubleListParameter(MAXIMA_ID, true);
      if(config.grab(maximaP)) {
        maxima = maximaP.getValue().clone();
      }

      config.checkConstraint(new AllOrNoneMustBeSetGlobalConstraint(minimaP, maximaP));
      config.checkConstraint(new EqualSizeGlobalConstraint(minimaP, maximaP));
    }

    @Override
    protected AttributeWiseMinMaxNormalization<V> makeInstance() {
      return new AttributeWiseMinMaxNormalization<>(minima, maxima);
    }
  }
}
