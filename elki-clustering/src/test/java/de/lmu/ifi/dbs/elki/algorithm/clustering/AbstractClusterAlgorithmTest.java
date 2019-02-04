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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Abstract unit test for clustering algorithms.
 *
 * Includes code for cluster evaluation.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractClusterAlgorithmTest extends AbstractSimpleAlgorithmTest {
  /**
   * Find a clustering result, fail if there is more than one or none.
   *
   * @param result Base result
   * @return Clustering
   */
  protected Clustering<?> findSingleClustering(Result result) {
    List<Clustering<? extends Model>> clusterresults = Clustering.getClusteringResults(result);
    assertTrue("No unique clustering found in result.", clusterresults.size() == 1);
    Clustering<? extends Model> clustering = clusterresults.get(0);
    return clustering;
  }

  /**
   * Test the clustering result by comparing the score with an expected value.
   *
   * @param database Database to test
   * @param clustering Clustering result
   * @param expected Expected score
   */
  protected <O> void testFMeasure(Database database, Clustering<?> clustering, double expected) {
    // Run by-label as reference
    ByLabelClustering bylabel = new ByLabelClustering();
    Clustering<Model> rbl = bylabel.run(database);

    ClusterContingencyTable ct = new ClusterContingencyTable(true, false);
    ct.process(clustering, rbl);
    double score = ct.getPaircount().f1Measure();
    Logging.getLogger(this.getClass()).verbose(this.getClass().getSimpleName() + " score: " + score + " expect: " + expected);
    assertEquals(this.getClass().getSimpleName() + ": Score does not match.", expected, score, 0.0001);
  }

  /**
   * Validate the cluster sizes with an expected result.
   *
   * @param clustering Clustering to test
   * @param expected Expected cluster sizes
   */
  protected void testClusterSizes(Clustering<?> clustering, int[] expected) {
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    int[] sizes = new int[clusters.size()];
    for(int i = 0; i < sizes.length; ++i) {
      sizes[i] = clusters.get(i).size();
    }
    // Sort both
    Arrays.sort(sizes);
    Arrays.sort(expected);
    // Test
    assertEquals("Number of clusters does not match expectations. " + FormatUtil.format(sizes), expected.length, sizes.length);
    for(int i = 0; i < expected.length; i++) {
      assertEquals("Cluster size does not match at position " + i + " in " + FormatUtil.format(sizes), expected[i], sizes[i]);
    }
  }
}
