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
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Normalization designed for data with a <em>meaningful zero</em>:<br>
 * The 0 is retained, and the data is linearly scaled to have a mean of 1,
 * by projection with f(x) = x / mean(X).
 * <p>
 * Each attribute is processed separately.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - NumberVector
 *
 * @param <V> vector type
 */
public class AttributeWiseMeanNormalization<V extends NumberVector> extends AbstractVectorConversionFilter<V, V> implements Normalization<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AttributeWiseMeanNormalization.class);

  /**
   * Stores the mean in each dimension.
   */
  private double[] mean = null;

  /**
   * Temporary storage used during initialization.
   */
  double[] sums = null;

  /**
   * Count the number of values seen.
   */
  int c = 0;

  /**
   * Constructor.
   * 
   * @param mean Mean value
   */
  public AttributeWiseMeanNormalization(double[] mean) {
    super();
    this.mean = mean;
  }

  /**
   * Constructor.
   */
  public AttributeWiseMeanNormalization() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    return (mean == null || mean.length == 0);
  }

  @Override
  protected void prepareProcessInstance(V featureVector) {
    // First object? Then init. (We didn't have a dimensionality before!)
    if(sums == null || sums.length == 0) {
      sums = new double[featureVector.getDimensionality()];
    }
    for(int d = 0; d < featureVector.getDimensionality(); d++) {
      sums[d] += featureVector.doubleValue(d);
    }
    ++c;
  }

  @Override
  protected void prepareComplete() {
    StringBuilder buf = LOG.isVerbose() ? new StringBuilder(200) : null;
    final int dimensionality = sums.length;
    mean = new double[dimensionality];
    if(buf != null) {
      buf.append("Normalization parameters: ");
    }
    for(int d = 0; d < dimensionality; d++) {
      mean[d] = sums[d] / c;
      if(buf != null) {
        buf.append(" m: ").append(mean[d]);
      }
    }
    sums = null;
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
    return val / mean[d];
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
    return val * mean[d];
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
        sum += (coeff_r[col[c]] /= mean[c]);
      }
      rhs[row[r]] += sum;
    }

    return new LinearEquationSystem(coeff, rhs, row, col);
  }

  @Override
  public String toString() {
    return new StringBuilder(200) //
        .append("normalization class: ").append(getClass().getName()).append('\n')//
        .append("normalization means: ").append(FormatUtil.format(mean))//
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
}
