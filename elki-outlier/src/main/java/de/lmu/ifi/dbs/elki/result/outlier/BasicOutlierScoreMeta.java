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

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Basic outlier score. Straightforward implementation of the
 * {@link OutlierScoreMeta} interface.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class BasicOutlierScoreMeta implements OutlierScoreMeta {
  /**
   * Store the actual minimum
   */
  double actualMinimum = Double.NaN;

  /**
   * Store the actual maximum
   */
  double actualMaximum = Double.NaN;

  /**
   * Store the theoretical minimum
   */
  double theoreticalMinimum = Double.NaN;

  /**
   * Store the theoretical maximum
   */
  double theoreticalMaximum = Double.NaN;

  /**
   * Store the theoretical baseline
   */
  double theoreticalBaseline = Double.NaN;

  /**
   * Constructor with actual values only.
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum) {
    this(actualMinimum, actualMaximum, Double.NaN, Double.NaN, Double.NaN);
  }

  /**
   * Constructor with all range values
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   * @param theoreticalMinimum theoretical minimum
   * @param theoreticalMaximum theoretical maximum
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum) {
    this(actualMinimum, actualMaximum, theoreticalMinimum, theoreticalMaximum, Double.NaN);
  }

  /**
   * Full constructor - all values.
   * 
   * @param actualMinimum actual minimum
   * @param actualMaximum actual maximum
   * @param theoreticalMinimum theoretical minimum
   * @param theoreticalMaximum theoretical maximum
   * @param theoreticalBaseline theoretical baseline
   */
  public BasicOutlierScoreMeta(double actualMinimum, double actualMaximum, double theoreticalMinimum, double theoreticalMaximum, double theoreticalBaseline) {
    super();
    if(Double.isNaN(actualMinimum) || Double.isNaN(actualMaximum)) {
      Logging.getLogger(this.getClass()).warning("Warning: Outlier Score meta initalized with NaN values: " + actualMinimum + " - " + actualMaximum);
    }
    this.actualMinimum = actualMinimum;
    this.actualMaximum = actualMaximum;
    this.theoreticalMinimum = theoreticalMinimum;
    this.theoreticalMaximum = theoreticalMaximum;
    this.theoreticalBaseline = theoreticalBaseline;
  }

  @Override
  public double getActualMaximum() {
    return actualMaximum;
  }

  @Override
  public double getActualMinimum() {
    return actualMinimum;
  }

  @Override
  public double getTheoreticalBaseline() {
    return theoreticalBaseline;
  }

  @Override
  public double getTheoreticalMaximum() {
    return theoreticalMaximum;
  }

  @Override
  public double getTheoreticalMinimum() {
    return theoreticalMinimum;
  }

  @Override
  public double normalizeScore(double value) {
    double center = 0.0;
    if(!Double.isNaN(theoreticalBaseline) && !Double.isInfinite(theoreticalBaseline)) {
      center = theoreticalBaseline;
    }
    else if(!Double.isNaN(theoreticalMinimum) && !Double.isInfinite(theoreticalMinimum)) {
      center = theoreticalMinimum;
    }
    else if(!Double.isNaN(actualMinimum) && !Double.isInfinite(actualMinimum)) {
      center = actualMinimum;
    }
    if(value < center) {
      return 0.0;
    }
    double max = Double.NaN;
    if(!Double.isNaN(theoreticalMaximum) && !Double.isInfinite(theoreticalMaximum)) {
      max = theoreticalMaximum;
    }
    else if(!Double.isNaN(actualMaximum) && !Double.isInfinite(actualMaximum)) {
      max = actualMaximum;
    }
    if(!Double.isNaN(max) && !Double.isInfinite(max) && max >= center) {
      return (value - center) / (max - center);
    }
    return value - center;
  }

  /**
   * @param actualMinimum the actualMinimum to set
   */
  public void setActualMinimum(double actualMinimum) {
    this.actualMinimum = actualMinimum;
  }

  /**
   * @param actualMaximum the actualMaximum to set
   */
  public void setActualMaximum(double actualMaximum) {
    this.actualMaximum = actualMaximum;
  }

  @Override
  public String getLongName() {
    return "Outlier Score Metadata";
  }

  @Override
  public String getShortName() {
    return "outlier-score-meta";
  }
}