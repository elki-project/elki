package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.index.AbstractIndexStructureTest;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexRangeQuery;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the M-tree.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MTreeTest extends AbstractIndexStructureTest {
  /**
   * Test {@link MTree} using a file based database connection.
   */
  @Test
  public void testMetrical() {
    ListParameterization metparams = new ListParameterization();
    metparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, MTreeFactory.class);
    metparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactEuclidean(metparams, MetricalIndexKNNQuery.class, MetricalIndexRangeQuery.class);
  }
}
