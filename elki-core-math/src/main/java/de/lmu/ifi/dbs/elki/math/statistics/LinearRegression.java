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
package de.lmu.ifi.dbs.elki.math.statistics;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * 
 * @author Arthur Zimek
 * @since 0.1
 */
// TODO: arthur comment
// TODO: use covariance matrix, add incremental API?
public class LinearRegression {
  private double t;

  private double m;

  /**
   * @param points Points to process
   */
  public LinearRegression(List<DoubleDoublePair> points) {
    double sumy = 0.0;
    double sumx = 0.0;
    //double sumyy = 0.0;
    double sumxx = 0.0;
    double sumxy = 0.0;
    int gap = 0;
    for(DoubleDoublePair point : points) {
      sumy += point.second;
      //sumyy += point.second * point.second;
      gap++;
      sumx += point.first;
      sumxx += point.first * point.first;
      sumxy += point.first * point.second;
    }
    double Sxy = sumxy - sumx * sumy / gap;
    double Sxx = sumxx - sumx * sumx / gap;
    m = Sxy / Sxx;
    t = (sumy - m * sumx) / gap;
  }

  /**
   * @return m
   */
  public double getM() {
    return this.m;
  }

  /**
   * @return t
   */
  public double getT() {
    return this.t;
  }
}