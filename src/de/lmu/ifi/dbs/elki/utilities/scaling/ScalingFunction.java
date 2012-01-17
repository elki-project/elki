package de.lmu.ifi.dbs.elki.utilities.scaling;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for scaling functions used e.g. by outlier evaluation such as
 * Histograms and visualization.
 * 
 * When using outlier scaling functions, make sure to invoke
 * {@link de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction#prepare
 * prepare} to invoke the preprocessing step!
 * 
 * Outside of an outlier context, you probably want to use
 * {@link StaticScalingFunction} instead, which may not require a
 * {@code prepare} statement.
 * 
 * @author Erich Schubert
 */
public interface ScalingFunction extends Parameterizable {
  /**
   * Transform a given value using the scaling function.
   * 
   * @param value Original value
   * @return Scaled value
   */
  public double getScaled(double value);

  /**
   * Get minimum resulting value. May be {@link Double#NaN} or
   * {@link Double#NEGATIVE_INFINITY}.
   * 
   * @return Minimum resulting value.
   */
  public double getMin();

  /**
   * Get maximum resulting value. May be {@link Double#NaN} or
   * {@link Double#POSITIVE_INFINITY}.
   * 
   * @return Maximum resulting value.
   */
  public double getMax();
}