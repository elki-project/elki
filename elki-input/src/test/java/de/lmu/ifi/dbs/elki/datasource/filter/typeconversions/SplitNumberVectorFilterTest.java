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
package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the split number vector filter filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class SplitNumberVectorFilterTest extends AbstractDataSourceTest {
  /**
   * Test with parameter s as a list of the columns to split into the first
   * bundle column.
   */
  @Test
  public void parameters() {
    String s = "0,1,2,3,4";
    int s_int = 5;
    String filename = UNITTEST + "dimensionality-test-1.csv";
    SplitNumberVectorFilter<DoubleVector> filter = new ELKIBuilder<>(SplitNumberVectorFilter.class) //
        .with(SplitNumberVectorFilter.Parameterizer.SELECTED_ATTRIBUTES_ID, s).build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));

    // Verify that the filter has split the columns represented by s into the
    // bundle's first column.
    Object obj = filteredBundle.data(0, 0);
    assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
    DoubleVector d = (DoubleVector) obj;
    assertEquals("Unexpected dimensionality", s_int, d.getDimensionality());
  }
}
