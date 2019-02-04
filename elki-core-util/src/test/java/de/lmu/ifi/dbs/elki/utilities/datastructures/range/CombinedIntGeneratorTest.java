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

import static de.lmu.ifi.dbs.elki.utilities.datastructures.range.ParseIntRangesTest.collect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

/**
 * Test of combined iterator.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CombinedIntGeneratorTest {
  @Test
  public void testCombined() {
    StaticIntGenerator fixed = new StaticIntGenerator(1, 2, 3);
    CombinedIntGenerator empty = new CombinedIntGenerator(Collections.emptyList());
    LinearIntGenerator linear = new LinearIntGenerator(10, 5, 20);
    ExponentialIntGenerator exponential = new ExponentialIntGenerator(50, 5, 1250);

    CombinedIntGenerator combined = new CombinedIntGenerator(empty, fixed, empty, exponential, linear);
    int[] sortedAnswer = { 1, 2, 3, 50, 250, 1250, 10, 15, 20 };
    String ser = "1,2,3,50,*=5,1250,10,+=5,20";

    assertEquals("Minimum wrong", 1, combined.getMin());
    assertEquals("Maximum wrong", 1250, combined.getMax());

    assertArrayEquals("Generated list does not match.", sortedAnswer, collect(combined));
    assertArrayEquals("Generated list does not match.", sortedAnswer, collect(combined));

    assertEquals("Serialization incorrect.", ser, combined.serializeTo(new StringBuilder()).toString());
    IntGenerator parsed = ParseIntRanges.parseIntRanges(ser);
    assertArrayEquals("Prased generator does not match.", sortedAnswer, collect(parsed));
  }
}
