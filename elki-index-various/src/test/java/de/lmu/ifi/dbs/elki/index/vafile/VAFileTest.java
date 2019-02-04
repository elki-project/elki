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
package de.lmu.ifi.dbs.elki.index.vafile;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.index.AbstractIndexStructureTest;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(VAFile.Factory.PARTITIONS_ID, 4).build();
    testExactEuclidean(factory, VAFile.VAFileKNNQuery.class, VAFile.VAFileRangeQuery.class);
    testSinglePoint(factory, VAFile.VAFileKNNQuery.class, VAFile.VAFileRangeQuery.class);
  }
}
