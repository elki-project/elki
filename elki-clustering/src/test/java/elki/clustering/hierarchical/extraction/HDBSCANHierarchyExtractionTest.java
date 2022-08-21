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
package elki.clustering.hierarchical.extraction;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.HDBSCANLinearMemory;
import elki.clustering.hierarchical.MiniMaxNNChain;
import elki.clustering.hierarchical.SLINK;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

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
        .with(HDBSCANHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build();
    assertFMeasure(db, slink.autorun(db), 0.9407684);
    assertClusterSizes(slink.autorun(db), new int[] { 8, 62, 104, 156 });
  }

  @Test
  public void testSLINKDegenerate() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 1) //
        .with(Algorithm.Utils.ALGORITHM_ID, SLINK.class) //
        .build();
    assertFMeasure(db, slink.autorun(db), 0.497315);
    assertClusterSizes(slink.autorun(db), new int[] { 0, 1, 329 });
  }

  @Test
  public void testHDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, HDBSCANLinearMemory.class) //
        .with(HDBSCANLinearMemory.Par.MIN_PTS_ID, 20) //
        .build();
    assertFMeasure(db, slink.autorun(db), 0.9703989);
    assertClusterSizes(slink.autorun(db), new int[] { 20, 55, 103, 152 });
  }

  @Test
  public void testMiniMaxNNResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    HDBSCANHierarchyExtraction slink = new ELKIBuilder<>(HDBSCANHierarchyExtraction.class) //
        .with(HDBSCANHierarchyExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, MiniMaxNNChain.class) //
        .build();
    assertFMeasure(db, slink.autorun(db), 0.91459);
    assertClusterSizes(slink.autorun(db), new int[] { 0, 59, 112, 159 });
  }
}
