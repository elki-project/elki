package de.lmu.ifi.dbs.elki.index.tree.metrical.covertree;

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
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractTestIndexStructures;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the Cover-tree.
 *
 * @author Erich Schubert
 */
public class TestCoverTree extends AbstractTestIndexStructures {
  /**
   * Test {@link CoverTree} using a file based database connection.
   */
  @Test
  public void testCovertree() {
    ListParameterization metparams = new ListParameterization();
    metparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, CoverTree.Factory.class);
    metparams.addParameter(CoverTree.Factory.Parameterizer.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.class);
    testExactEuclidean(metparams, CoverTree.CoverTreeKNNQuery.class, CoverTree.CoverTreeRangeQuery.class);
  }
}
