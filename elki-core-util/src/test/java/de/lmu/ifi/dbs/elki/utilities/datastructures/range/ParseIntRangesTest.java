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
package de.lmu.ifi.dbs.elki.utilities.datastructures.range;

import static de.lmu.ifi.dbs.elki.utilities.datastructures.range.ParseIntRanges.parseIntRanges;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;

/**
 * Test for the parser of integer ranges
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ParseIntRangesTest {
  @Test
  public void examples() {
    IntGenerator r;
    r = parseIntRanges("1,2,3,...,10");
    assertArrayEquals("Simple", new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, collect(r));
    assertTrue("Not simplified", r instanceof LinearIntGenerator);
    r = parseIntRanges("1,3,,10");
    assertArrayEquals("Variants", new int[] { 1, 3, 5, 7, 9 }, collect(r));
    r = parseIntRanges("1,3,..,10");
    assertArrayEquals("Variants", new int[] { 1, 3, 5, 7, 9 }, collect(r));
    r = parseIntRanges("1,3,...,10");
    assertArrayEquals("Variants", new int[] { 1, 3, 5, 7, 9 }, collect(r));
    r = parseIntRanges("1,+=2,10");
    assertArrayEquals("Variants", new int[] { 1, 3, 5, 7, 9 }, collect(r));
    r = parseIntRanges("1,*=2,16");
    assertArrayEquals("Exponential", new int[] { 1, 2, 4, 8, 16 }, collect(r));
    r = parseIntRanges("1,2,3,4,..,10,100");
    assertArrayEquals("Extra1", new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100 }, collect(r));
    r = parseIntRanges("100,1,3,..,10");
    assertArrayEquals("Extra2", new int[] { 100, 1, 3, 5, 7, 9 }, collect(r));
    r = parseIntRanges("1,2,..,10,20,..,100,200,..,1000");
    assertArrayEquals("Continuations", new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000 }, collect(r));
  }

  public static int[] collect(IntGenerator g) {
    IntegerArray out = new IntegerArray();
    g.forEach(out::add);
    return out.toArray();
  }
}
