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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the MAD normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class AttributeWiseMADNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    AttributeWiseMADNormalization<DoubleVector> filter = new ELKIBuilder<AttributeWiseMADNormalization<DoubleVector>>(AttributeWiseMADNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Count how many values in each column are positive, how many are negative,
    // and how many are greater than 1, or less than -1.
    int[][] counts = new int[dim][4];

    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double val = d.doubleValue(col);
        counts[col][val > 0. ? 0 : 1]++;
        counts[col][Math.abs(val) >= NormalDistribution.PHIINV075 ? 2 : 3]++;
      }
    }

    // Verify that ~50% of the values in each column are negative (=> ~50% of
    // the values are positive).
    // Verify that ~50% of the values are either greater than 1 or less than -1.
    for(int col = 0; col < dim; col++) {
      assertEquals("~50% of the values in each column should be positive", .5, counts[col][0] / (double) bundle.dataLength(), 0.);
      assertEquals("~50% of the values in each column should be negative", .5, counts[col][1] / (double) bundle.dataLength(), 0.);
      assertEquals("~50% of the values in each column should be > 1 or < -1", .5, counts[col][2] / (double) bundle.dataLength(), 0.);
      assertEquals("~50% of the values in each column should be -1 to +1", .5, counts[col][3] / (double) bundle.dataLength(), 0.);
    }
  }
}
