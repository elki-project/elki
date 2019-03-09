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
package elki.index.tree.spatial.rstarvariants.xtree;

import org.junit.Test;

import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNQuery;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeQuery;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the X-tree index.
 * 
 * Note: the test currently will not run in Gradle, but it works in Eclipse. The
 * reason is that it depends on test classes and data from the main 'elki'
 * module, but these are not exported into the jar (which is correct).
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class XTreeTest extends AbstractIndexStructureTest {
  /**
   * Test {@link XTree} using a file based database connection.
   */
  @Test
  public void testXTree() {
    XTreeFactory<?> factory = new ELKIBuilder<>(XTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300).build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }
}
