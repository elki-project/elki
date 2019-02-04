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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
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
 * @assoc - - - DistributionEstimator
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
    // Clone the samples:
    double[] x = toPrimitiveDoubleArray(data, adapter);
    // We first need the basic parameters:
    final int len = x.length;
    final int num = ((int) (len * trim));
    final int cut1 = num >> 1, cut2 = num - cut1;
    // Partially sort our copy.
    QuickSelect.quickSelect(x, 0, len, cut1);
    QuickSelect.quickSelect(x, cut1, len, len - 1 - cut2);
    // Trimmed estimate:
    return inner.estimate(x, new DoubleArrayAdapter() {
      @Override
      public double getDouble(double[] array, int off) throws IndexOutOfBoundsException {
        return x[off + cut1];
      }

      @Override
      public int size(double[] array) {
        return len - num;
      }
    });
  }

  /**
   * Local copy, see ArrayLikeUtil.toPrimitiveDoubleArray.
   * 
   * @param data Data
   * @param adapter Adapter
   * @return Copy of the data, as {@code double[]}
   */
  public static <A> double[] toPrimitiveDoubleArray(A data, NumberArrayAdapter<?, A> adapter) {
    if(adapter == DoubleArrayAdapter.STATIC) {
      return ((double[]) data).clone();
    }
    final int len = adapter.size(data);
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      x[i] = adapter.getDouble(data, i);
    }
    return x;
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
   * @hidden
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

      DoubleParameter trimP = new DoubleParameter(TRIM_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
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
