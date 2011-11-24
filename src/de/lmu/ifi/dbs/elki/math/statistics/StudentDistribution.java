package de.lmu.ifi.dbs.elki.math.statistics;

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

import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Tabelarizes the values for student distribution.
 * 
 * @author Elke Achtert
 */
public class StudentDistribution {
  /**
   * Available alpha values.
   */
  public static double _6000 = 0.6;

  /**
   * Available alpha values.
   */
  public static double _8000 = 0.8;

  /**
   * Available alpha values.
   */
  public static double _9000 = 0.9;

  /**
   * Available alpha values.
   */
  public static double _9500 = 0.95;

  /**
   * Available alpha values.
   */
  public static double _9750 = 0.975;

  /**
   * Available alpha values.
   */
  public static double _9900 = 0.99;

  /**
   * Available alpha values.
   */
  public static double _9950 = 0.995;

  /**
   * Available alpha values.
   */
  public static double _9990 = 0.999;

  /**
   * Available alpha values.
   */
  public static double _9995 = 0.9995;

  /**
   * Available alpha values.
   */
  public static double _4000 = 0.4;

  /**
   * Available alpha values.
   */
  public static double _2000 = 0.2;

  /**
   * Available alpha values.
   */
  public static double _1000 = 0.1;

  /**
   * Available alpha values.
   */
  public static double _0500 = 0.05;

  /**
   * Available alpha values.
   */
  public static double _0250 = 0.025;

  /**
   * Available alpha values.
   */
  public static double _0100 = 0.01;

  /**
   * Available alpha values.
   */
  public static double _0050 = 0.005;

  /**
   * Available alpha values.
   */
  public static double _0010 = 0.001;

  /**
   * Available alpha values.
   */
  public static double _0005 = 0.005;

  /**
   * Holds the t-values.
   */
  private static TIntObjectMap<TDoubleDoubleMap> tValues = new TIntObjectHashMap<TDoubleDoubleMap>();

  static {
    put(31, new double[] { 0.2533, 0.8416, 1.2816, 1.6449, 1.96, 2.3263, 2.5758, 3.0903, 3.2906 });
  }

  /**
   * Returns the t-value for the given alpha-value and degree of freedom.
   * 
   * @param alpha the alpha value
   * @param n the degree of freedom
   * @return the t-value for the given alpha-value and degree of freedom
   */
  public static double tValue(double alpha, int n) {
    if(n > 30) {
      n = 31;
    }
    TDoubleDoubleMap map = tValues.get(n);
    if(map == null) {
      throw new IllegalArgumentException("t-values for n=" + n + " not yet tabularized!");
    }

    Double value = map.get(alpha);
    if(value == null) {
      throw new IllegalArgumentException("t-values for alpha=" + alpha + " not tabularized!");
    }

    return value;
  }

  /**
   * Stores the specified t-values for the given degree of freedom.
   * 
   * @param n the degree of freedom
   * @param values the t-values
   */
  private static void put(int n, double[] values) {
    TDoubleDoubleMap map = new TDoubleDoubleHashMap();
    map.put(_6000, values[0]);
    map.put(_8000, values[1]);
    map.put(_9000, values[2]);
    map.put(_9500, values[3]);
    map.put(_9750, values[4]);
    map.put(_9900, values[5]);
    map.put(_9950, values[6]);
    map.put(_9990, values[7]);
    map.put(_9995, values[8]);

    map.put(_4000, -values[0]);
    map.put(_2000, -values[1]);
    map.put(_1000, -values[2]);
    map.put(_0500, -values[3]);
    map.put(_0250, -values[4]);
    map.put(_0100, -values[5]);
    map.put(_0050, -values[6]);
    map.put(_0010, -values[7]);
    map.put(_0005, -values[8]);
    tValues.put(n, map);
  }
}