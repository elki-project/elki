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
package elki.datasource.filter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.data.type.TypeUtil;
import elki.datasource.AbstractDataSourceTest;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.utilities.ELKIBuilder;

/**
 * Test the pass-through dummy filter.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class NoOpFilterTest extends AbstractDataSourceTest {
  @Test
  public void passthrough() {
    String filename = UNITTEST + "normalization-test-1.csv";
    NoOpFilter filter = new ELKIBuilder<>(NoOpFilter.class).build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);

    // Check dimensionality
    assertEquals("Dimensionality", getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD), getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD));

    // Verify that approximately p% of the values were sampled.
    assertEquals("Unexpected bundle length", unfilteredBundle.dataLength(), filteredBundle.dataLength());
  }
}
