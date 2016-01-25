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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.DistributionEstimator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Winsorising or Georgization estimator. Similar to trimming, this is expected
 * to be more robust to outliers. However, instead of removing the extreme
 * values, they are instead replaced with the cutoff value. This keeps the
 * quantity of the data the same, and will have a lower impact on variance and
 * similar measures.
 * 
 * Reference:
 * <p>
 * C. Hastings, F. Mosteller, J. W. Tukey, C. P. Winsor<br />
 * Low moments for small samples: a comparative study of order statistics.<br />
 * The Annals of Mathematical Statistics, 18(3) *
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.uses DistributionEstimator
 *
 * @param <D> Distribution type
 */
@Reference(authors = "C. Hastings, F. Mosteller, J. W. Tukey, C. P. Winsor", title = "Low moments for small samples: a comparative study of order statistics", booktitle = "The Annals of Mathematical Statistics, 18(3)", url = "http://dx.doi.org/10.1214/aoms/1177730388")
public class WinsorisingEstimator<D extends Distribution> implements DistributionEstimator<D> {
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
  public WinsorisingEstimator(DistributionEstimator<D> inner, double winsorize) {
    super();
    this.inner = inner;
    this.winsorize = winsorize;
  }

  @Override
  public <A> D estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // We first need the basic parameters:
    int len = adapter.size(data);
    final int cut = ((int) (len * winsorize)) >> 1;
    // X positions of samples
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      x[i] = val;
    }
    // Partially sort our copy.
    double min = QuickSelect.quickSelect(x, 0, len, cut);
    double max = QuickSelect.quickSelect(x, cut, len, len - 1 - cut);
    // Winsorize by replacing the smallest and largest values.
    // QuickSelect ensured that these are correctly in place.
    for(int i = 0, j = len - 1; i < cut; i++, j--) {
      x[i] = min;
      x[j] = max;
    }
    return inner.estimate(x, ArrayLikeUtil.DOUBLEARRAYADAPTER);
  }

  @Override
  public Class<? super D> getDistributionClass() {
    return inner.getDistributionClass();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(" + inner.toString() + ", trim=" + winsorize + ")";
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

      DoubleParameter trimP = new DoubleParameter(WINSORIZE_ID);
      trimP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      trimP.addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
      if(config.grab(trimP)) {
        winsorize = trimP.doubleValue();
      }
    }

    @Override
    protected WinsorisingEstimator<D> makeInstance() {
      return new WinsorisingEstimator<>(inner, winsorize);
    }
  }
}
