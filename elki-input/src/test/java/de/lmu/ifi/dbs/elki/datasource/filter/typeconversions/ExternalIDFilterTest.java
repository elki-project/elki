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

import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the external ID filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class ExternalIDFilterTest extends AbstractDataSourceTest {
  /**
   * Test with parameter c as the column whose label is to be extacted.
   */
  @Test
  public void parameters() {
    final int c = 2;
    String filename = UNITTEST + "external-id-test-1.csv";
    ExternalIDFilter filter = new ELKIBuilder<>(ExternalIDFilter.class) //
        .with(ExternalIDFilter.Parameterizer.EXTERNALID_INDEX_ID, c).build();
    MultipleObjectsBundle bundle = readBundle(filename, filter);

    // Ensure that the filter has correctly formed the bundle.
    // We expect that the bundle's first column is a number vector field.
    // We expect that the bundle's second column is an ExternalID
    // We expect that the bundle's third column is a LabelList object.

    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));

    // Ensure that the second column are the ExternalID objects.
    Object obj = bundle.data(0, 1);
    assertEquals("Unexpected data type", ExternalID.class, obj.getClass());
    // Ensure that the length of the list of ExternalID objects has the correct
    // length.
    assertEquals("Unexpected data length", bundle.dataLength(), bundle.getColumn(1).size());

    // Ensure that the third column are the LabelList objects.
    obj = bundle.data(0, 2);
    assertEquals("Unexpected data type", LabelList.class, obj.getClass());
  }
}
