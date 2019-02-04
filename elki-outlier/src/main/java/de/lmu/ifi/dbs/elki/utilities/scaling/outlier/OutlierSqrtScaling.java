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
package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Scaling that can map arbitrary positive values to a value in the range of
 * [0:1].
 * <p>
 * Transformation is done by taking the square root, then doing a linear linear
 * mapping onto 0:1 using the minimum values seen.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class OutlierSqrtScaling implements OutlierScaling {
  /**
   * Minimum and maximum values.
   */
  protected double min, max;

  /**
   * Predefined minimum and maximum values.
   */
  protected Double pmin = null, pmax = null;

  /**
   * Scaling factor
   */
  protected double factor;

  /**
   * Constructor.
   * 
   * @param pmin Predefined minimum
   * @param pmax Predefined maximum
   */
  public OutlierSqrtScaling(Double pmin, Double pmax) {
    super();
    this.pmin = pmin;
    this.pmax = pmax;
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    return value <= min ? 0. : Math.min(1, (FastMath.sqrt(value - min) / factor));
  }

  @Override
  public void prepare(OutlierResult or) {
    if(pmin == null || pmax == null) {
      DoubleMinMax mm = new DoubleMinMax();
      DoubleRelation scores = or.getScores();
      for(DBIDIter id = scores.iterDBIDs(); id.valid(); id.advance()) {
        double val = scores.doubleValue(id);
        if(!Double.isInfinite(val)) {
          mm.put(val);
        }
      }
      min = (pmin == null) ? mm.getMin() : pmin;
      max = (pmax == null) ? mm.getMax() : pmax;
    }
    factor = FastMath.sqrt(max - min);
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    if(pmin == null || pmax == null) {
      DoubleMinMax mm = new DoubleMinMax();
      final int size = adapter.size(array);
      for(int i = 0; i < size; i++) {
        double val = adapter.getDouble(array, i);
        if(!Double.isInfinite(val)) {
          mm.put(val);
        }
      }
      min = (pmin == null) ? mm.getMin() : pmin;
      max = (pmax == null) ? mm.getMax() : pmax;
    }
    factor = FastMath.sqrt(max - min);
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the fixed minimum to use.
     */
    public static final OptionID MIN_ID = new OptionID("sqrtscale.min", "Fixed minimum to use in sqrt scaling.");

    /**
     * Parameter to specify the fixed maximum to use.
     */
    public static final OptionID MAX_ID = new OptionID("sqrtscale.max", "Fixed maximum to use in sqrt scaling.");

    /**
     * Predefined minimum value.
     */
    protected double min;

    /**
     * Predefined maximum value.
     */
    protected double max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minP = new DoubleParameter(MIN_ID) //
          .setOptional(true);
      if(config.grab(minP)) {
        min = minP.getValue();
      }
      DoubleParameter maxP = new DoubleParameter(MAX_ID) //
          .setOptional(true);
      if(config.grab(maxP)) {
        max = maxP.getValue();
      }
    }

    @Override
    protected OutlierSqrtScaling makeInstance() {
      return new OutlierSqrtScaling(min, max);
    }
  }
}
