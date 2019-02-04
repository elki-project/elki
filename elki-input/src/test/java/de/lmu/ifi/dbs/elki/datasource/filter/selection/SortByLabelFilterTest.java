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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the label sorting filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class SortByLabelFilterTest extends AbstractDataSourceTest {
  /**
   * Test with default parameters.
   */
  @Test
  public void defaultParameters() {
    String filename = UNITTEST + "label-selection-test-1.csv";
    SortByLabelFilter filter = new ELKIBuilder<>(SortByLabelFilter.class).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);
    // Expect vectors to come first, labels second.
    getFieldDimensionality(bundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);

    // Verify that the vectors are in alphabetical order.
    for(int row = 0; row < bundle.dataLength() - 1; row++) {
      LabelList llFirst = get(bundle, row, 1, LabelList.class);
      LabelList llSecond = get(bundle, row + 1, 1, LabelList.class);
      assertTrue("Expected alphabetical order", llFirst.get(0).compareToIgnoreCase(llSecond.get(0)) <= 0);
    }
  }
}
