package de.lmu.ifi.dbs.elki.distance.distancevalue;

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

import java.util.regex.Pattern;

/**
 * An abstract distance implements equals conveniently for any extending class.
 * At the same time any extending class is to implement hashCode properly.
 * 
 * See {@link de.lmu.ifi.dbs.elki.distance.DistanceUtil} for related utility
 * functions such as <code>min</code>, <code>max</code>.
 * 
 * @author Arthur Zimek
 * @see de.lmu.ifi.dbs.elki.distance.DistanceUtil
 * @param <D> the (final) type of Distance used
 */
public abstract class AbstractDistance<D extends AbstractDistance<D>> implements Distance<D> {
  /**
   * Indicates an infinity pattern.
   */
  public static final String INFINITY_PATTERN = "inf";
  
  /**
   * Pattern for parsing and validating double values
   */
  public static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+|\\d*\\.\\d+)?([eE][-]?\\d+)?");
  
  /**
   * Pattern for parsing and validating integer values
   */
  public static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  /**
   * Any extending class should implement a proper hashCode method.
   */
  @Override
  public abstract int hashCode();

  /**
   * Returns true if <code>this == o</code> has the value <code>true</code> or o
   * is not null and o is of the same class as this instance and
   * <code>this.compareTo(o)</code> is 0, false otherwise.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }

    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    return this.compareTo((D) o) == 0;
  }

  /**
   * Get the pattern accepted by this distance
   * 
   * @return Pattern
   */
  abstract public Pattern getPattern();
  
  @Override
  public final String requiredInputPattern() {
    return getPattern().pattern();
  }
  
  /**
   * Test a string value against the input pattern.
   * 
   * @param value String value to test
   * @return Match result
   */
  public final boolean testInputPattern(String value) {
    return getPattern().matcher(value).matches();
  }

  @Override
  public boolean isInfiniteDistance() {
    return this.equals(infiniteDistance());
  }

  @Override
  public boolean isNullDistance() {
    return this.equals(nullDistance());
  }

  @Override
  public boolean isUndefinedDistance() {
    return this.equals(undefinedDistance());
  }
}