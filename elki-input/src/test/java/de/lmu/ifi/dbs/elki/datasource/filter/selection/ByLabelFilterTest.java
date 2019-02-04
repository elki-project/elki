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

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the label selection filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class ByLabelFilterTest extends AbstractDataSourceTest {
  /**
   * Test with parameter s as the label to look for.
   */
  @Test
  public void parameters() {
    String s = "yes";
    String filename = UNITTEST + "label-selection-test-1.csv";
    ByLabelFilter filter = new ELKIBuilder<>(ByLabelFilter.class) //
        .with(ByLabelFilter.Parameterizer.LABELFILTER_PATTERN_ID, s) //
        .build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertEquals("Dimensionality", getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD), getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD));

    // Verify that the filter selected all vectors which match the pattern.
    int count_match = 0;
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      LabelList ll = get(unfilteredBundle, row, 1, LabelList.class);
      if(ll.get(0).equals(s)) {
        count_match++;
      }
    }

    assertTrue("Expected at least one match", count_match > 0);
    assertEquals("Unexpected number of matches", count_match, filteredBundle.dataLength());
  }
}
