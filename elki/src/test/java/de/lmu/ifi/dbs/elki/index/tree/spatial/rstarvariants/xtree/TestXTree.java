package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.index.AbstractTestIndexStructures;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeQuery;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the X-tree index.
 * 
 * @author Erich Schubert
 */
public class TestXTree extends AbstractTestIndexStructures {
  /**
   * Test {@link XTree} using a file based database connection.
   */
  @Test
  public void testXTree() {
    ListParameterization xtreeparams = new ListParameterization();
    xtreeparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, XTreeFactory.class);
    xtreeparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactEuclidean(xtreeparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }
}
