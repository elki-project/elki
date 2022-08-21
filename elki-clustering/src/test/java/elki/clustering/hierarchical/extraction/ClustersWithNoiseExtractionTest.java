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
import elki.clustering.hierarchical.AGNES;
import elki.clustering.hierarchical.Anderberg;
import elki.clustering.hierarchical.NNChain;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for k clusters with minsize extraction.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ClustersWithNoiseExtractionTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testClustering() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(ClustersWithNoiseExtraction.class) //
        .with(ClustersWithNoiseExtraction.Par.K_ID, 3) //
        .with(ClustersWithNoiseExtraction.Par.MINCLUSTERSIZE_ID, 5) //
        .with(Algorithm.Utils.ALGORITHM_ID, Anderberg.class) //
        .with(AGNES.Par.LINKAGE_ID, WardLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9242);
    assertClusterSizes(clustering, new int[] { 56, 123, 151 });
  }

  @Test
  public void testNNChain() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<?> clustering = new ELKIBuilder<>(ClustersWithNoiseExtraction.class) //
        .with(ClustersWithNoiseExtraction.Par.K_ID, 3) //
        .with(ClustersWithNoiseExtraction.Par.MINCLUSTERSIZE_ID, 5) //
        .with(Algorithm.Utils.ALGORITHM_ID, NNChain.class) //
        .with(NNChain.Par.LINKAGE_ID, WardLinkage.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.9242);
    assertClusterSizes(clustering, new int[] { 56, 123, 151 });
  }
}
