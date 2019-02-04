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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the histogram jitter filter.
 *
 * @author Matthew Arcifa
 * @since 0.7.5
 */
public class HistogramJitterFilterTest extends AbstractDataSourceTest {
  /**
   * Test with seed of 0 and given jitter amount.
   */
  @Test
  public void parameters() {
    String filename = UNITTEST + "transformation-test-1.csv";
    // Use the value of s as the seed value and j as the jitter amount.
    final double s = 0.;
    final double j = .01;
    HistogramJitterFilter<DoubleVector> filter = new ELKIBuilder<>(HistogramJitterFilter.class) //
        .with(HistogramJitterFilter.Parameterizer.SEED_ID, s) //
        .with(HistogramJitterFilter.Parameterizer.JITTER_ID, j) //
        .build();
    MultipleObjectsBundle filteredBundle = readBundle(filename, filter);
    // Load the test data again without a filter.
    MultipleObjectsBundle unfilteredBundle = readBundle(filename);
    int dim = getFieldDimensionality(filteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD);
    assertEquals("Dimensionality changed", dim, getFieldDimensionality(unfilteredBundle, 0, TypeUtil.NUMBER_VECTOR_FIELD));
    // Verify that the filtered and unfiltered bundles have the same length.
    assertEquals("Test file interpreted incorrectly", filteredBundle.dataLength(), unfilteredBundle.dataLength());

    // Verify that at least p% of the values are within a% of the unfiltered
    // value.
    final double p = .9, a = .1;
    int withinRange = 0;
    for(int row = 0; row < filteredBundle.dataLength(); row++) {
      DoubleVector dFil = get(filteredBundle, row, 0, DoubleVector.class);
      DoubleVector dUnfil = get(unfilteredBundle, row, 0, DoubleVector.class);
      for(int col = 0; col < dim; col++) {
        final double vFil = dFil.doubleValue(col);
        final double vUnfil = dUnfil.doubleValue(col);
        if(Math.abs((vFil / vUnfil) - 1.) <= a) {
          withinRange++;
        }
      }
    }
    assertEquals("Too many values have moved too much", 1., withinRange / (double) (dim * filteredBundle.dataLength()), 1. - p);
  }
}
