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
package de.lmu.ifi.dbs.elki.result.outlier;

import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Generic meta information about the value range of an outlier score.
 * 
 * All values can be {@link Double#NaN} if not specified by the algorithm or not
 * computed (actual values). For theoretical values, it is explicitly allowed to
 * return {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public interface OutlierScoreMeta extends Result {
  /**
   * Get the actual minimum of the value range.
   * 
   * @return minimum or {@link Double#NaN}
   */
  double getActualMinimum();

  /**
   * Get the actual maximum of the value range.
   * 
   * @return maximum or {@link Double#NaN}
   */
  double getActualMaximum();

  /**
   * Get the theoretical minimum of the value range.
   * 
   * @return theoretical minimum or {@link Double#NaN}
   */
  double getTheoreticalMinimum();

  /**
   * Get the theoretical maximum of the value range.
   * 
   * This value may be {@link Double#NEGATIVE_INFINITY} or {@link Double#NaN}.
   * 
   * @return theoretical maximum or {@link Double#NaN}
   */
  double getTheoreticalMaximum();

  /**
   * Get the theoretical baseline of the value range.
   * 
   * It will be common to see {@link Double#POSITIVE_INFINITY} here.
   * 
   * @return theoretical baseline or {@link Double#NaN}
   */
  double getTheoreticalBaseline();

  /**
   * Return a normalized value of the outlier score.
   * 
   * @param value outlier score
   * @return Normalized value (in 0.0-1.0)
   */
  double normalizeScore(double value);
}
