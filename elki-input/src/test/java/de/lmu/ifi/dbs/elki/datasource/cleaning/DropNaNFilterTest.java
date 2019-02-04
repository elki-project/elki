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
package de.lmu.ifi.dbs.elki.datasource.cleaning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.cleaning.DropNaNFilter;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the NaN-drop cleaning filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class DropNaNFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "nan-test-1.csv";
    DropNaNFilter filter = new ELKIBuilder<>(DropNaNFilter.class).build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Get dimensionalities
    int dimFiltered = getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);
    int dimUnfiltered = getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Ensure that at least a single NaN exists in the unfiltered bundle.
    boolean NaNfound = false;
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      DoubleVector d = get(unfilteredBundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dimUnfiltered; col++) {
        final double v = d.doubleValue(col);
        if(Double.isNaN(v)) {
          NaNfound = true;
          break;
        }
      }
    }
    assertTrue("NaN expected in unfiltered data", NaNfound);

    // Ensure that no single NaN exists in the filtered bundle.
    for(int row = 0; row < filteredBundle.dataLength(); row++) {
      DoubleVector d = get(filteredBundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dimFiltered; col++) {
        assertFalse("NaN not expected", Double.isNaN(d.doubleValue(col)));
      }
    }
  }
}
