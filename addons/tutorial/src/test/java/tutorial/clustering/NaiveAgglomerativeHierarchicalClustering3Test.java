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
package tutorial.clustering;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.data.Clustering;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test agglomerative hierarchical clustering, using the naive algorithm.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class NaiveAgglomerativeHierarchicalClustering3Test extends AbstractClusterAlgorithmTest {
  @Test
  public void testSingleLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.SINGLE) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertClusterSizes(clustering, new int[] { 9, 200, 429 });
    assertFMeasure(db, clustering, 0.6829722);
  }

  @Test
  public void testWard() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.WARD) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testGroupAverage() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.GROUP_AVERAGE) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testWeightedAverage() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.WEIGHTED_AVERAGE) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testCompleteLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.COMPLETE) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.938167802);
    assertClusterSizes(clustering, new int[] { 200, 217, 221 });
  }

  @Test
  public void testCentroid() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.CENTROID) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.93866265);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testMedian() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering3<?>>(NaiveAgglomerativeHierarchicalClustering3.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(NaiveAgglomerativeHierarchicalClustering3.Par.LINKAGE_ID, NaiveAgglomerativeHierarchicalClustering3.Linkage.MEDIAN) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertFMeasure(db, clustering, 0.9386626);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }
}
