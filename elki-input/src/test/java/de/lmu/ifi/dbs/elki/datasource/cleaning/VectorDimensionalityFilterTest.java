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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.cleaning.VectorDimensionalityFilter;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the vector dimensionality cleaning filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class VectorDimensionalityFilterTest extends AbstractDataSourceTest {
  /**
   * Test with parameter dim_keep as the dimensionality of the vectors to leave.
   */
  @Test
  public void parameters() {
    final int dim_keep = 10;
    String filename = UNITTEST + "dimensionality-test-2.csv";
    VectorDimensionalityFilter<DoubleVector> filter = new ELKIBuilder<VectorDimensionalityFilter<DoubleVector>>(VectorDimensionalityFilter.class) //
        .with(VectorDimensionalityFilter.Parameterizer.DIM_P, dim_keep).build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);

    // Verify that the filter has removed the vectors of the wrong
    // dimensionality.
    boolean foundTooSmall = false;
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      Object obj = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      if(d.getDimensionality() != dim_keep) {
        foundTooSmall = true;
        break;
      }
    }
    assertTrue("Expected a vector with filterable dimensionality", foundTooSmall);

    assertTrue("Expected smaller data length", filteredBundle.dataLength() < unfilteredBundle.dataLength());
  }
}
