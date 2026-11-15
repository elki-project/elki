/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.hierarchical;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.relation.Relation;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.distance.minkowski.EuclideanDistance;
import elki.utilities.ELKIBuilder;

/**
 * Perform agglomerative hierarchical clustering, using the index accelerated
 * Boruvka-based algorithm.
 *
 * @author Erich Schubert
 */
public class HDBSCANBorvukaSTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSingleLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, HDBSCANBoruvkaS.class) //
        .with(HDBSCANLinearMemory.Par.MIN_PTS_ID, 20) //
        .build().autorun(db);
    assertClusterSizes(clustering, new int[] { 1, 200, 437 });
    assertFMeasure(db, clustering, 0.686953412);
  }

  /**
   * Exact duplicates are merged during initialization; the result must still
   * match the reference implementation.
   */
  @Test
  public void testDuplicates() {
    double[][] data = new double[150][];
    for(int i = 0, p = 0; i < data.length; p++) {
      double[] v = { (p * 0.6180339887) % 1., (p * 0.4142135624) % 1. };
      for(int r = 0; r <= p % 4 && i < data.length; r++) {
        data[i++] = v;
      }
    }
    Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data), null);
    db.initialize();
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    double[] expected = sortedHeights(new HDBSCANLinearMemory<NumberVector>(EuclideanDistance.STATIC, 5).run(rel));
    double[] actual = sortedHeights(new HDBSCANBoruvkaS<NumberVector>(EuclideanDistance.STATIC, 5).run(rel));
    assertArrayEquals(expected, actual, 1e-15);
  }

  private static double[] sortedHeights(ClusterMergeHistory merges) {
    double[] heights = new double[merges.numMerges()];
    for(int i = 0; i < heights.length; i++) {
      heights[i] = merges.getMergeHeight(i);
    }
    Arrays.sort(heights);
    return heights;
  }
}
