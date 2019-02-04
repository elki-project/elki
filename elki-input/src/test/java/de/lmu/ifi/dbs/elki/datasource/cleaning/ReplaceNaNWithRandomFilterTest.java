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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.cleaning.ReplaceNaNWithRandomFilter;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the random NaN-replacement cleaning filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class ReplaceNaNWithRandomFilterTest extends AbstractDataSourceTest {
  /**
   * Test with standard normal distribution as parameter.
   */
  @Test
  public void parameters() {
    String filename = UNITTEST + "nan-test-1.csv";
    ReplaceNaNWithRandomFilter filter = new ELKIBuilder<>(ReplaceNaNWithRandomFilter.class) //
        .with(ReplaceNaNWithRandomFilter.Parameterizer.REPLACEMENT_DISTRIBUTION, //
            new NormalDistribution(0, 1, new Random(0L))).build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));
    // This cast is now safe (vector field):
    int dimFiltered = ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality();
    int dimUnfiltered = ((FieldTypeInformation) unfilteredBundle.meta(0)).getDimensionality();
    assertEquals("Dimensionality expected equal", dimFiltered, dimUnfiltered);

    // Note the indices of the NaN(s) in the data.
    List<IntegerVector> NaNs = new ArrayList<IntegerVector>();
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      Object obj = unfilteredBundle.data(row, 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      for(int col = 0; col < dimUnfiltered; col++) {
        final double v = d.doubleValue(col);
        if(Double.isNaN(v)) {
          NaNs.add(new IntegerVector(new int[] { row, col }));
        }
      }
    }
    // Verify that at least a single NaN exists in the unfiltered bundle.
    assertTrue("NaN expected in unfiltered data", NaNs.size() > 0);

    for(IntegerVector iv : NaNs) {
      Object obj = filteredBundle.data(iv.intValue(0), 0);
      assertEquals("Unexpected data type", DoubleVector.class, obj.getClass());
      DoubleVector d = (DoubleVector) obj;
      final double v = d.doubleValue(iv.intValue(1));
      assertFalse("NaN not expected", Double.isNaN(v));
    }
  }
}
