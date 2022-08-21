/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.vafile;

import org.junit.Test;

import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the VAfile index.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class VAFileTest extends AbstractIndexStructureTest {
  /**
   * Test {@link VAFile} using a file based database connection.
   */
  @Test
  public void testVAFile() {
    VAFile.Factory<?> factory = new ELKIBuilder<>(VAFile.Factory.class) //
        .with(VAFile.Factory.Par.PARTITIONS_ID, 4).build();
    assertExactEuclidean(factory, VAFile.VAFileKNNQuery.class, VAFile.VAFileRangeQuery.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
