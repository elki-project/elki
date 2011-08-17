package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Scaling that can map arbitrary positive values to a value in the range of
 * [0:1].
 * 
 * Transformation is done by taking the square root, then doing a linear linear
 * mapping onto 0:1 using the minimum values seen.
 * 
 * @author Erich Schubert
 */
public class OutlierSqrtScaling implements OutlierScalingFunction {
  /**
   * Parameter to specify the fixed minimum to use.
   * <p>
   * Key: {@code -sqrtscale.min}
   * </p>
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("sqrtscale.min", "Fixed minimum to use in sqrt scaling.");

  /**
   * Parameter to specify the fixed maximum to use.
   * <p>
   * Key: {@code -sqrtscale.max}
   * </p>
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("sqrtscale.max", "Fixed maximum to use in sqrt scaling.");

  /**
   * Field storing the minimum value
   */
  protected Double min = null;

  /**
   * Field storing the Maximum value
   */
  protected Double max = null;

  /**
   * Scaling factor
   */
  protected double factor;

  /**
   * Constructor.
   * 
   * @param min
   * @param max
   */
  public OutlierSqrtScaling(Double min, Double max) {
    super();
    this.min = min;
    this.max = max;
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    if(value <= min) {
      return 0;
    }
    return Math.min(1, (Math.sqrt(value - min) / factor));
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    if(min == null || max == null) {
      DoubleMinMax mm = new DoubleMinMax();
      for(DBID id : ids) {
        double val = or.getScores().get(id);
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          mm.put(val);
        }
      }
      if(min == null) {
        min = mm.getMin();
      }
      if(max == null) {
        max = mm.getMax();
      }
    }
    factor = Math.sqrt(max - min);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected double min;

    protected double max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minP = new DoubleParameter(MIN_ID, true);
      if(config.grab(minP)) {
        min = minP.getValue();
      }
      DoubleParameter maxP = new DoubleParameter(MAX_ID, true);
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