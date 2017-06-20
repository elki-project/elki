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
 * Test the label sorting filter.
 *
 * @author Matthew Arcifa
 */
public class SortByLabelFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters. 
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "label-selection-test-1.csv";
    // Allow loading test data from resources.
    SortByLabelFilter filter = ClassGenericsUtil.parameterizeOrAbort(SortByLabelFilter.class, new ListParameterization());
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));

    // Verify that the vectors are in alphabetical order.
    for(int row = 0; row < bundle.dataLength() - 1; row++) {
      Object objFirst = bundle.data(row, 1);
      assertEquals("Unexpected data type", LabelList.class, objFirst.getClass());
      LabelList llFirst = (LabelList) objFirst;
      Object objSecond = bundle.data(row + 1, 1);
      assertEquals("Unexpected data type", LabelList.class, objSecond.getClass());
      LabelList llSecond = (LabelList) objSecond;
      assertTrue("Expected alphabetical order", llFirst.get(0).compareToIgnoreCase(llSecond.get(0)) <= 0);
    }
  }
}
