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
package de.lmu.ifi.dbs.elki.index.invertedlist;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.index.AbstractIndexStructureTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for the iDistance index.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class InMemoryInvertedIndexTest extends AbstractIndexStructureTest {
  /**
   * Test {@link InMemoryInvertedIndex}.
   */
  @Test
  public void testInvertedIndex() {
    // We could have used "new InMemoryInvertedIndex.Factory()", but we also
    // want to test the parameterizer code.
    InMemoryInvertedIndex.Factory<?> factory = new ELKIBuilder<>(InMemoryInvertedIndex.Factory.class).build();
    testExactCosine(factory, InMemoryInvertedIndex.CosineKNNQuery.class, InMemoryInvertedIndex.CosineRangeQuery.class);
  }
}
