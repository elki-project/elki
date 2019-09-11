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
package elki.clustering.hierarchical.extraction;

import org.junit.Test;

import elki.AbstractAlgorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.AnderbergHierarchicalClustering;
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
        .with(AbstractAlgorithm.ALGORITHM_ID, AnderbergHierarchicalClustering.class) //
        .build().run(db);
    testFMeasure(db, clustering, 0.9242);
    testClusterSizes(clustering, new int[] { 56, 123, 151 });
  }
}
