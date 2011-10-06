package de.lmu.ifi.dbs.elki.math;

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
/**
 * Class to incrementally compute pearson correlation.
 * 
 * @author Erich Schubert
 */
public class PearsonCorrelation {
  /**
   * Sum for XX
   */
  private double sumXX = 0;

  /**
   * Sum for YY
   */
  private double sumYY = 0;

  /**
   * Sum for XY
   */
  private double sumXY = 0;

  /**
   * Current mean for X
   */
  private double meanX;

  /**
   * Current mean for Y
   */
  private double meanY;

  /**
   * Weight sum
   */
  private double sumWe = 0;

  /**
   * Constructor.
   */
  public PearsonCorrelation() {
    super();
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   * @param w Weight
   */
  public void put(double x, double y, double w) {
    if(sumWe <= 0.0) {
      meanX = x;
      meanY = y;
      sumWe = w;
      return;
    }
    // Incremental update
    sumWe += w;
    // Delta to previous mean
    final double deltaX = x - meanX;
    final double deltaY = y - meanY;
    // Update means
    meanX += deltaX * w / sumWe;
    meanY += deltaY * w / sumWe;
    // Delta to new mean
    final double neltaX = x - meanX;
    final double neltaY = y - meanY;
    // Update
    sumXX += w * deltaX * neltaX;
    sumYY += w * deltaY * neltaY;
    // should equal weight * deltaY * neltaX!
    sumXY += w * deltaX * neltaY;
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   */
  public void put(double x, double y) {
    put(x, y, 1.0);
  }

  /**
   * Get the pearson correlation value.
   * 
   * @return Correlation value
   */
  public double getCorrelation() {
    final double popSdX = Math.sqrt(sumXX / sumWe);
    final double popSdY = Math.sqrt(sumYY / sumWe);
    final double covXY = sumXY / sumWe;
    if(popSdX == 0 || popSdY == 0) {
      return 0;
    }
    return covXY / (popSdX * popSdY);
  }
}