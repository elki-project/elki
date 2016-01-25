package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Trimmed wrapper around other estimators. Sorts the data, trims it, then
 * analyzes it using another estimator.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.uses DistributionEstimator
 * 
 * @param <D> Distribution type
 */
public class TrimmedEstimator<D extends Distribution> implements DistributionEstimator<D> {
  /**
   * Distribution estimator to use.
   */
  private DistributionEstimator<D> inner;

  /**
   * Amount of data to trim.
   */
  private double trim;

  /**
   * Constructor.
   * 
   * @param inner Inner estimator.
   * @param trim Trimming parameter.
   */
  public TrimmedEstimator(DistributionEstimator<D> inner, double trim) {
    super();
    this.inner = inner;
    this.trim = trim;
  }

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // We first need the basic parameters:
    int len = adapter.size(data);
    final int cut = ((int) (len * trim)) >> 1;
    // X positions of samples
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      x[i] = val;
    }
    // Sort our copy.
    Arrays.sort(x);
    { // Trim:
      // TODO: is it more efficient to just copy, or instead use a trimmed array
      // adapter?
      double[] trimmed = new double[len - 2 * cut];
      System.arraycopy(x, cut, trimmed, 0, trimmed.length);
      x = trimmed;
      len = trimmed.length;
    }
    return inner.estimate(x, ArrayLikeUtil.DOUBLEARRAYADAPTER);
  }

  @Override
  public Class<? super D> getDistributionClass() {
    return inner.getDistributionClass();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(" + inner.toString() + ", trim=" + trim + ")";
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <D> Distribution type
   */
  public static class Parameterizer<D extends Distribution> extends AbstractParameterizer {
    /**
     * Option for the class to use on the trimmed sample.
     */
    public static final OptionID INNER_ID = new OptionID("trimmedestimate.inner", "Estimator to use on the trimmed data.");

    /**
     * Option for specifying the amount of data to trim.
     */
    public static final OptionID TRIM_ID = new OptionID("trimmedestimate.trim", "Relative amount of data to trim on each end, must be 0 < trim < 0.5");

    /**
     * Distribution estimator to use.
     */
    private DistributionEstimator<D> inner;

    /**
     * Amount of data to trim.
     */
    private double trim;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistributionEstimator<D>> innerP = new ObjectParameter<>(INNER_ID, DistributionEstimator.class);
      if(config.grab(innerP)) {
        inner = innerP.instantiateClass(config);
      }

      DoubleParameter trimP = new DoubleParameter(TRIM_ID);
      trimP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      trimP.addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
      if(config.grab(trimP)) {
        trim = trimP.doubleValue();
      }
    }

    @Override
    protected TrimmedEstimator<D> makeInstance() {
      return new TrimmedEstimator<>(inner, trim);
    }
  }
}
