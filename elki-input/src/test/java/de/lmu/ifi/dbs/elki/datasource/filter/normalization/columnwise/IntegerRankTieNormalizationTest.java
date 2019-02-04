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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;

/**
 * Test the integer rank tie normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class IntegerRankTieNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    IntegerRankTieNormalization filter = new ELKIBuilder<>(IntegerRankTieNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    IntegerArray coldata = new IntegerArray(bundle.dataLength());

    for(int col = 0; col < dim; col++) {
      coldata.clear();
      // Extract the column:
      for(int row = 0; row < bundle.dataLength(); row++) {
        IntegerVector obj = get(bundle, row, 0, IntegerVector.class);
        coldata.add(obj.intValue(col));
      }
      // Sort values:
      coldata.sort();
      // Verify that the gap matches the frequency of each value.
      final int size = coldata.size;
      assertEquals("First value", coldata.get(0), coldata.get(coldata.get(0)));
      for(int i = 0; i < size;) {
        // s: Start, i: end, v: value, f: frequency
        int s = i, v = coldata.get(i), f = 1;
        while(++i < size && v == coldata.get(i)) {
          f++;
        }
        // Only iff the frequencies is even, the values will be odd.
        assertNotSame("Even/odd rule", (f & 1), (v & 1));
        assertEquals("Bad value at position " + s, s + i - 1, v);
        assertEquals("Bad frequency at position " + s, i - s, f);
      }
    }
  }
}
