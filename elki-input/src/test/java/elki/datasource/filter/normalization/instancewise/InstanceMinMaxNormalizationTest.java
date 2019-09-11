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
package elki.datasource.filter.normalization.instancewise;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.type.TypeUtil;
import elki.datasource.AbstractDataSourceTest;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.math.DoubleMinMax;
import elki.utilities.ELKIBuilder;

/**
 * Test the min-max normalization filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class InstanceMinMaxNormalizationTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "normalization-test-1.csv";
    InstanceMinMaxNormalization<DoubleVector> filter = new ELKIBuilder<>(InstanceMinMaxNormalization.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    int dim = getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Verify that, in each row, the min value is 0 and the max value 1.
    DoubleMinMax mms = new DoubleMinMax();
    for(int row = 0; row < bundle.dataLength(); row++) {
      mms.reset();
      DoubleVector d = get(bundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        mms.put(d.doubleValue(col));
      }
      assertEquals("Min value is not 0", 0., mms.getMin(), 0);
      assertEquals("Max value is not 1", 1., mms.getMax(), 1e-15);
    }
  }
}
