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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Winsorizing or Georgization estimator. Similar to trimming, this is supposed
 * to be more robust to outliers. However, instead of removing the extreme
 * values, they are instead replaced with the cutoff value. This keeps the
 * quantity of the data the same, and will have a lower impact on variance and
 * similar measures.
 * <p>
 * Reference:
 * <p>
 * C. Hastings, F. Mosteller, J. W. Tukey, C. P. Winsor<br>
 * Low moments for small samples: a comparative study of order statistics.<br>
 * The Annals of Mathematical Statistics, 18(3) *
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - DistributionEstimator
 *
 * @param <D> Distribution type
 */
@Reference(authors = "C. Hastings, F. Mosteller, J. W. Tukey, C. P. Winsor", //
    title = "Low moments for small samples: a comparative study of order statistics", //
    booktitle = "The Annals of Mathematical Statistics, 18(3)", //
    url = "https://doi.org/10.1214/aoms/1177730388", //
    bibkey = "doi:10.1214/aoms/1177730388")
@Alias({ "de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.meta.WinsorisingEstimator" })
public class WinsorizingEstimator<D extends Distribution> implements DistributionEstimator<D> {
  /**
   * Distribution estimator to use.
   */
  private DistributionEstimator<D> inner;

  /**
   * Amount of data to winsorize.
   */
  private double winsorize;

  /**
   * Constructor.
   * 
   * @param inner Inner estimator.
   * @param winsorize Winsorize parameter.
   */
  public WinsorizingEstimator(DistributionEstimator<D> inner, double winsorize) {
    super();
    this.inner = inner;
    this.winsorize = winsorize;
  }

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // Clone the samples:
    double[] x = TrimmedEstimator.toPrimitiveDoubleArray(data, adapter);
    // We first need the basic parameters:
    final int len = x.length;
    final int num = ((int) (len * winsorize));
    final int cut1 = num >> 1, cut2 = num - cut1;
    // Partially sort our copy.
    double min = QuickSelect.quickSelect(x, 0, len, cut1);
    double max = QuickSelect.quickSelect(x, cut1, len, len - 1 - cut2);
    // Winsorize by replacing the smallest and largest values.
    // QuickSelect ensured that these are correctly in place.
    Arrays.fill(x, 0, cut1, min);
    Arrays.fill(x, len - cut2, len, max);
    return inner.estimate(x, DoubleArrayAdapter.STATIC);
  }

  @Override
  public Class<? super D> getDistributionClass() {
    return inner.getDistributionClass();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(" + inner.toString() + ", winsorize=" + winsorize + ")";
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
     * Option for the class to use on the winsorized sample.
     */
    public static final OptionID INNER_ID = new OptionID("winsorize.inner", "Estimator to use on the winsorized data.");

    /**
     * Option for specifying the amount of data to winsorize.
     */
    public static final OptionID WINSORIZE_ID = new OptionID("winsorize.winsorize", "Relative amount of data to winsorize on each end, must be 0 < winsorize < 0.5");

    /**
     * Distribution estimator to use.
     */
    private DistributionEstimator<D> inner;

    /**
     * Amount of data to winsorize.
     */
    private double winsorize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistributionEstimator<D>> innerP = new ObjectParameter<>(INNER_ID, DistributionEstimator.class);
      if(config.grab(innerP)) {
        inner = innerP.instantiateClass(config);
      }

      DoubleParameter trimP = new DoubleParameter(WINSORIZE_ID)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
      if(config.grab(trimP)) {
        winsorize = trimP.doubleValue();
      }
    }

    @Override
    protected WinsorizingEstimator<D> makeInstance() {
      return new WinsorizingEstimator<>(inner, winsorize);
    }
  }
}
