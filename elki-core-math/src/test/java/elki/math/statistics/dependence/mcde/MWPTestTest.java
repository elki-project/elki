/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.dependence.mcde;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Test for the Mann-Whitney based p-test.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 * @since 0.8.0
 */
public class MWPTestTest {
  @Test
  public void testRanking() {
    MWPTest mwpTest = MWPTest.STATIC;
    DoubleArrayAdapter adapter = DoubleArrayAdapter.STATIC;
    double[] input_no_duplicates = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    MWPTest.MWPRanking result_no_duplicates = mwpTest.correctedRanks(adapter, input_no_duplicates, 10);

    for(int i = 0; i < 10; i++) {
      assertTrue("ranks incorrect", input_no_duplicates[i] == result_no_duplicates.adjusted[i]);
    }

    double[] input_duplicates_middle = { 0, 1, 1, 2, 3, 4, 5, 5, 5, 6, 7, 8, 9 };
    MWPTest.MWPRanking output_duplicates_middle = mwpTest.correctedRanks(adapter, input_duplicates_middle, 13);
    assertEquals("ranks incorrect", output_duplicates_middle.adjusted[1], 1.5, 0);
    assertEquals("ranks incorrect", output_duplicates_middle.adjusted[2], 1.5, 0);
    assertEquals("ranks incorrect", output_duplicates_middle.adjusted[6], 7.0, 0);
    assertEquals("ranks incorrect", output_duplicates_middle.adjusted[7], 7.0, 0);
    assertEquals("ranks incorrect", output_duplicates_middle.adjusted[8], 7.0, 0);

    double[] input_duplicates_start_end = { 0, 0, 0, 2, 3, 4, 5, 5, 5, 6, 9, 9, 9 };
    MWPTest.MWPRanking output_duplicates_start_end = mwpTest.correctedRanks(adapter, input_duplicates_start_end, 13);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[0], 1.0, 0);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[1], 1.0, 0);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[2], 1.0, 0);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[10], 11.0, 0);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[11], 11.0, 0);
    assertEquals("ranks incorrect", output_duplicates_start_end.adjusted[12], 11.0, 0);
  }

  @Test
  public void testStatTest() {
    int len = 10;
    double[] data = new double[len];
    Arrays.fill(data, 1.0);
    boolean[] slice = { false, false, true, true, true, true, true, false, false, false };

    MWPTest.MWPRanking ranks = MWPTest.STATIC.correctedRanks(DoubleArrayAdapter.STATIC, data, len);
    assertEquals("MWPTest.statisticalTest() returns NaN", //
        MWPTest.STATIC.statisticalTest(0, 5, slice, ranks), 0, 0);
  }
}
