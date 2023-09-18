/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.scaling.outlier;

import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;

/**
 * Scaling that can map arbitrary values to a value in the range of [0:1].
 * <p>
 * Transformation is done by linear mapping onto 0:1 using the minimum and
 * maximum values.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class OutlierLinearScaling implements OutlierScaling {
  /**
   * Field storing the Minimum to use
   */
  private double min;

  /**
   * Field storing the Maximum value
   */
  private double max;

  /**
   * Scaling factor to use 1/(max-min)
   */
  private double ifactor;

  /**
   * Use the mean for scaling
   */
  private boolean usemean = false;

  /**
   * Ignore zero values
   */
  private boolean nozeros = false;

  /**
   * Constructor.
   */
  public OutlierLinearScaling() {
    this(Double.NaN, Double.NaN, false, false);
  }

  /**
   * Constructor.
   * 
   * @param min Minimum value (may be NaN to get from the data)
   * @param max Maximum value (may be NaN to get from the data)
   * @param usemean Use the mean as minimum for scaling
   * @param nozeros Ignore zero values
   */
  public OutlierLinearScaling(double min, double max, boolean usemean, boolean nozeros) {
    super();
    this.min = min;
    this.max = max;
    this.usemean = usemean;
    this.nozeros = nozeros;
    this.ifactor = (min == min && max == max) ? 1. / (max - min) : Double.NaN;
  }

  @Override
  public double getScaled(double value) {
    assert ifactor != 0 : "prepare() was not run prior to using the scaling function.";
    return ifactor < 0 ? //
        value >= max ? 0 : Math.min(1, ((value - max) * ifactor)) : //
        value <= min ? 0 : Math.min(1, ((value - min) * ifactor));
  }

  @Override
  public void prepare(OutlierResult or) {
    if(min != min /* NaN */ || max != max /* NaN */ || usemean) {
      double mi = Double.MAX_VALUE, ma = Double.MIN_VALUE, sum = 0;
      int count = 0, skippedzeros = 0;
      DoubleRelation scores = or.getScores();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        double val = scores.doubleValue(id);
        if(nozeros && val == 0.0) {
          ++skippedzeros;
          continue;
        }
        sum += val;
        ++count;
        mi = val < mi ? val : mi;
        ma = val > ma ? val : ma;
      }
      if(count == 0 && skippedzeros > 0) {
        sum = mi = ma = 0.; // constant zero data.
      }
      min = usemean && count > 0 ? (sum / count) : min == min /* not NaN */ ? min : mi;
      max = max == max /* not NaN */ ? max : ma;
    }
    ifactor = max > min ? 1. / (max - min) : 1.;
    if(or.getOutlierMeta() instanceof InvertedOutlierScoreMeta) {
      ifactor *= -1;
    }
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    if(min != min /* NaN */ || max != max /* NaN */ || usemean) {
      double mi = Double.MAX_VALUE, ma = Double.MIN_VALUE, sum = 0;
      int count = 0, skippedzeros = 0;
      final int size = adapter.size(array);
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        if(nozeros && val == 0.0) {
          ++skippedzeros;
          continue;
        }
        sum += val;
        ++count;
        mi = val < mi ? val : mi;
        ma = val > ma ? val : ma;
      }
      if(count == 0 && skippedzeros > 0) {
        sum = mi = ma = 0.; // constant zero data.
      }
      min = usemean && count > 0 ? (sum / count) : min == min /* not NaN */ ? min : mi;
      max = max == max /* not NaN */ ? max : ma;
    }
    ifactor = max > min ? 1. / (max - min) : 1.;
  }

  @Override
  public double getMin() {
    return 0.;
  }

  @Override
  public double getMax() {
    return 1.;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify a fixed minimum to use.
     */
    public static final OptionID MIN_ID = new OptionID("linearscale.min", "Fixed minimum to use in linear scaling.");

    /**
     * Parameter to specify the maximum value.
     */
    public static final OptionID MAX_ID = new OptionID("linearscale.max", "Fixed maximum to use in linear scaling.");

    /**
     * Flag to use the mean as minimum for scaling.
     */
    public static final OptionID MEAN_ID = new OptionID("linearscale.usemean", "Use the mean as minimum for scaling.");

    /**
     * Flag to use ignore zeros when computing the min and max.
     */
    public static final OptionID NOZEROS_ID = new OptionID("linearscale.ignorezero", "Ignore zero entries when computing the minimum and maximum.");

    /**
     * Field storing the Minimum to use
     */
    protected Double min = null;

    /**
     * Field storing the Maximum value
     */
    protected Double max = null;

    /**
     * Use the mean for scaling
     */
    boolean usemean = false;

    /**
     * Ignore zero values
     */
    boolean nozeros = false;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(MIN_ID).setOptional(true) //
          .grab(config, x -> min = x);
      new DoubleParameter(MAX_ID).setOptional(true) //
          .grab(config, x -> max = x);
      if(min == null && max == null) {
        new Flag(MEAN_ID).grab(config, x -> usemean = x);
      }
      new Flag(NOZEROS_ID).grab(config, x -> nozeros = x);
    }

    @Override
    public OutlierLinearScaling make() {
      return new OutlierLinearScaling(min, max, usemean, nozeros);
    }
  }
}
