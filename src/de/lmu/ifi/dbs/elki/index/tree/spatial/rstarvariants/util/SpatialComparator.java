package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

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

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Compares objects of type SpatialComparable.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses SpatialComparable
 */
public final class SpatialComparator implements Comparator<SpatialComparable> {
  /**
   * Indicates the comparison of the min values of the entries' MBRs.
   */
  public static final int MIN = 1;

  /**
   * Indicates the comparison of the max values of the entries' MBRs.
   */
  public static final int MAX = 2;

  /**
   * The dimension for comparison.
   */
  private final int compareDimension;

  /**
   * Indicates the comparison value (min or max).
   */
  private final int comparisonValue;

  /**
   * Creates a new spatial comparator with the specified parameters.
   * 
   * @param compareDimension the dimension to be set for comparison
   * @param comparisonValue the comparison value to be set
   */
  public SpatialComparator(int compareDimension, int comparisonValue) {
    this.compareDimension = compareDimension;
    this.comparisonValue = comparisonValue;
  }

  /**
   * Compares the two specified spatial comparables according to the sorting
   * dimension and the comparison value of this Comparator.
   * 
   * @param o1 the first spatial comparable
   * @param o2 the second spatial comparable
   * @return a negative integer, zero, or a positive integer as the first
   *         argument is less than, equal to, or greater than the second.
   */
  @Override
  public int compare(SpatialComparable o1, SpatialComparable o2) {
    if(comparisonValue == MIN) {
      if(o1.getMin(compareDimension) < o2.getMin(compareDimension)) {
        return -1;
      }

      if(o1.getMin(compareDimension) > o2.getMin(compareDimension)) {
        return +1;
      }
    }
    else if(comparisonValue == MAX) {
      if(o1.getMax(compareDimension) < o2.getMax(compareDimension)) {
        return -1;
      }

      if(o1.getMax(compareDimension) > o2.getMax(compareDimension)) {
        return +1;
      }
    }
    else {
      throw new IllegalArgumentException("No comparison value specified!");
    }
    return 0;
  }
}
