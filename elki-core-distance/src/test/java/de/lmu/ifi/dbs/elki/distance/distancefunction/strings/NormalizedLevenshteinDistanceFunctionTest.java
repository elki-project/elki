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
package de.lmu.ifi.dbs.elki.distance.distancefunction.strings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunctionTest;

/**
 * Unit test for Levenshtein distance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class NormalizedLevenshteinDistanceFunctionTest extends AbstractDistanceFunctionTest {
  /**
   * Some test strings, from the Wikipedia article.
   */
  final String[][] TESTS = { //
      { "kitten", "sitting" }, //
      { "Saturday", "Sunday" }, //
      { "tier", "tor" }, //
      { "abcz", "zabc" }, //
      { "zabc", "abcz" }, //
  };

  /**
   * The associated scores.
   */
  final double[] SCORES = { //
      3. / 6.5, // kitten <-> sitting
      3. / 7., // Saturday <-> Sunday
      2. / 3.5, // tier <-> tor
      2. / 4., // abcz <-> zabc
      2. / 4., // zabc <-> abcz
  };

  @Test
  public void testStringLevenshtein() {
    NormalizedLevenshteinDistanceFunction f = NormalizedLevenshteinDistanceFunction.STATIC_SENSITIVE;
    basicChecks(f);
    for(int i = 0; i < TESTS.length; i++) {
      assertEquals("Distance does not agree: " + TESTS[i][0] + " <-> " + TESTS[i][1], SCORES[i], f.distance(TESTS[i][0], TESTS[i][1]), 1E-10);
    }
  }

  @Test
  public void testTriangleInequalityCounterexample() {
    NormalizedLevenshteinDistanceFunction f = NormalizedLevenshteinDistanceFunction.STATIC_SENSITIVE;
    assertEquals("Distance ab - bc", 1., f.distance("ab", "bc"), 0.);
    assertEquals("Distance ab - abc", .4, f.distance("ab", "abc"), 0.);
    assertEquals("Distance abc - bc", .4, f.distance("abc", "bc"), 0.);
  }
}
