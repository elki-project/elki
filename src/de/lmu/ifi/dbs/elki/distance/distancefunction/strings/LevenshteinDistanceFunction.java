package de.lmu.ifi.dbs.elki.distance.distancefunction.strings;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Classic Levenshtein distance on strings.
 * 
 * TODO: add case insensitive flag.
 * 
 * @author Felix Stahlberg
 * @author Erich Schubert
 */
public class LevenshteinDistanceFunction extends AbstractPrimitiveDistanceFunction<String, IntegerDistance> {
  /**
   * Static instance, case sensitive.
   */
  public static final LevenshteinDistanceFunction STATIC_SENSITIVE = new LevenshteinDistanceFunction();

  /**
   * Constructor. Use static instance instead.
   */
  @Deprecated
  public LevenshteinDistanceFunction() {
    super();
  }

  @Override
  public IntegerDistance distance(String o1, String o2) {
    if (o1.equals(o2)) {
      return new IntegerDistance(0);
    }
    final int cost = levenshteinDistance(o1, o2);
    return new IntegerDistance(cost);
  }

  /**
   * Levenshtein distance for two strings.
   * 
   * @param o1 First string
   * @param o2 Second string
   * @return Levenshtein distance
   */
  public static int levenshteinDistance(String o1, String o2) {
    // Let o2 be the shorter one:
    if (o1.length() < o2.length()) {
      return levenshteinDistance(o2, o1);
    }
    final int l1 = o1.length(), l2 = o2.length();
    // Use two buffers:
    int[] curr = new int[l2 + 1], prev = new int[l2 + 1];
    // Initial row
    for (int j = 1; j <= l2; j++) {
      curr[j] = j;
    }
    for (int i = 0; i < l1; i++) {
      // Swap curr and prev:
      int[] tmp = curr;
      curr = prev;
      prev = tmp;
      // Compute next row:
      curr[0] = i;
      for (int j = 0; j < l2; j++) {
        // TODO: allow case insensitive comparisons?
        final int cost = (o1.charAt(i) == o2.charAt(j)) ? 0 : 1;
        curr[j + 1] = Math.min(prev[j + 1] + 1, Math.min(curr[j] + 1, prev[j] + cost));
      }
    }
    final int cost = curr[o2.length()];
    return cost;
  }

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }

  @Override
  public SimpleTypeInformation<? super String> getInputTypeRestriction() {
    return TypeUtil.STRING;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LevenshteinDistanceFunction makeInstance() {
      return STATIC_SENSITIVE;
    }
  }
}
