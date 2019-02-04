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
package de.lmu.ifi.dbs.elki.datasource.filter.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the shuffle objects filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class ShuffleObjectsFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "sorted-data-1.csv";
    ShuffleObjectsFilter filter = new ELKIBuilder<>(ShuffleObjectsFilter.class)//
        .with(ShuffleObjectsFilter.Parameterizer.SEED_ID, 0)//
        .build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertEquals("Dimensionality", getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD), getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD));
    assertEquals("Length changed", unfilteredBundle.dataLength(), filteredBundle.dataLength());

    // Verify that the elements of the unfiltered bundle are in sorted order.
    double prev = get(unfilteredBundle, 0, 0, DoubleVector.class).doubleValue(0);
    for(int row = 1; row < unfilteredBundle.dataLength(); row++) {
      final double next = get(unfilteredBundle, row, 0, DoubleVector.class).doubleValue(0);
      assertTrue("Values are expected to be in sorted order", prev <= next);
      prev = next;
    }

    // Verify that the elements of the filtered bundle are not in sorted order.
    // By verifying this, we can ascertain that the vectors have been shuffled.
    prev = get(filteredBundle, 0, 0, DoubleVector.class).doubleValue(0);
    boolean shuffled = false;
    for(int row = 1; row < filteredBundle.dataLength(); row++) {
      final double next = get(filteredBundle, row, 0, DoubleVector.class).doubleValue(0);
      if(prev > next) {
        shuffled = true;
        break;
      }
    }
    assertTrue("Elements are not shuffled.", shuffled);
  }
}
