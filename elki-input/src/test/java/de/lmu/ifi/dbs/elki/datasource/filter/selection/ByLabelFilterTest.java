/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the label selection filter.
 *
 * @author Matthew Arcifa
 */
public class ByLabelFilterTest extends AbstractDataSourceTest {
  /**
   * Test with parameter s as the label to look for. 
   */
  @Test
  public void parameters() {
    String s = "yes";
    String filename = UNITTEST + "label-selection-test-1.csv";
    // Allow loading test data from resources.
    ListParameterization config = new ListParameterization();
    config.addParameter(ByLabelFilter.Parameterizer.LABELFILTER_PATTERN_ID, s);
    ByLabelFilter filter = ClassGenericsUtil.parameterizeOrAbort(ByLabelFilter.class, config);
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(filteredBundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(unfilteredBundle.meta(0)));

    // Verify that the filter selected all vectors which match the pattern.
    int count_match = 0;
    for(int row = 0; row < unfilteredBundle.dataLength(); row++) {
      Object obj = unfilteredBundle.data(row, 1);
      assertEquals("Unexpected data type", LabelList.class, obj.getClass());
      LabelList ll = (LabelList) obj;
      if(ll.get(0).equals(s))
        count_match++;
    }
    
    assertTrue("Expected at least one match", count_match > 0);
    
    assertEquals("Unexpected number of matches", count_match, filteredBundle.dataLength());
  }
}
