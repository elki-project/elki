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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.MiniMaxNNChain;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for simplified hierarchy extraction.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HDBSCANHierarchyExtractionTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build();
    testFMeasure(db, slink.run(db), 0.9407684);
    testClusterSizes(slink.run(db), new int[] { 8, 62, 104, 156 });
  }

  @Test
  public void testSLINKDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 1) //
        .with(AbstractAlgorithm.ALGORITHM_ID, SLINK.class) //
        .build();
    testFMeasure(db, slink.run(db), 0.497315);
    testClusterSizes(slink.run(db), new int[] { 0, 1, 329 });
  }

  @Test
  public void testHDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50) //
        .with(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20) //
        .build();
    testFMeasure(db, slink.run(db), 0.97218);
    testClusterSizes(slink.run(db), new int[] { 21, 54, 103, 152 });
  }

  @Test
  public void testMiniMaxNNResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50) //
        .with(AbstractAlgorithm.ALGORITHM_ID, MiniMaxNNChain.class) //
        .build();
    testFMeasure(db, slink.run(db), 0.91459);
    testClusterSizes(slink.run(db), new int[] { 0, 59, 112, 159 });
  }
}
