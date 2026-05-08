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
import elki.clustering.hierarchical.ClusterMergeHistoryBuilder;
import elki.clustering.hierarchical.HDBSCANLinearMemory;
import elki.clustering.hierarchical.MiniMaxNNChain;
import elki.clustering.hierarchical.SLINK;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.result.Metadata;
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
    assertClusterSizes(slink.autorun(db), new int[] { 1, 329 });
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

  /**
   * Test GLOSH scores are invariant under tie-breaking at equal merge heights.
   * Consider points at coordinates 0, 2, 3, 5, 5.5 with minPts=2.
   * Two merges are tied at distance 2.
   */
  @Test
  public void testGLOSHComplexHierarchy() {
    DBIDRange ids = DBIDUtil.generateStaticDBIDRange(5);
    double cds[] = new double[] { 2.0, 1.0, 1.0, 0.5, 0.5 };
    WritableDoubleDataStore coredists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 3.0);

    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      coredists.putDouble(it, cds[ids.index(it)]);
    }

    ClusterMergeHistoryBuilder builder1 = new ClusterMergeHistoryBuilder(ids, false);
    {
      int de = builder1.add(3, 0.5, 4);
      int bc = builder1.add(1, 1.0, 2);
      int abc = builder1.add(bc, 2.0, 0);
      builder1.add(abc, 2.0, de);
    }
    ClusterMergeHistoryBuilder builder2 = new ClusterMergeHistoryBuilder(ids, false);
    {
      int de = builder2.add(3, 0.5, 4);
      int bc = builder2.add(1, 1.0, 2);
      int bcde = builder2.add(bc, 2.0, de);
      builder2.add(bcde, 2.0, 0);
    }

    Clustering<DendrogramModel> cluster1 = new HDBSCANHierarchyExtraction(null, 2, false).run(builder1.complete(coredists));
    Clustering<DendrogramModel> cluster2 = new HDBSCANHierarchyExtraction(null, 2, false).run(builder2.complete(coredists));

    WritableDoubleDataStore glosh1 = Metadata.hierarchyOf(cluster1).iterDescendants().filter(WritableDoubleDataStore.class).get();
    WritableDoubleDataStore glosh2 = Metadata.hierarchyOf(cluster2).iterDescendants().filter(WritableDoubleDataStore.class).get();
    assert glosh1 != null : "GLOSH scores not found in first clustering";
    assert glosh2 != null : "GLOSH scores not found in second clustering";

    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double score1 = glosh1.doubleValue(it), score2 = glosh2.doubleValue(it);
      assert Math.abs(score1 - score2) < 1e-10 : "GLOSH score mismatch for idx " + ids.index(it) + ": " + score1 + " vs " + score2;
    }
  }
}
