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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.instancewise;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the hellinger histogram normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class HellingerHistogramNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    HellingerHistogramNormalization<DoubleVector> filter = new ELKIBuilder<>(HellingerHistogramNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Verify that, in each vector, the sum of the squares is equal to 1.
    for(int row = 0; row < bundle.dataLength(); row++) {
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      double sum = 0.;
      for(int col = 0; col < dim; col++) {
        final double v = d.doubleValue(col);
        sum += v * v;
      }
      assertEquals("Sum of squares not as expected", .5, sum, 1e-15);
    }
  }
}
