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
package elki.clustering.hierarchical;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.clustering.optics.AbstractOPTICS;
import elki.clustering.optics.OPTICSHeap;
import elki.clustering.optics.OPTICSList;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test OPTICS via hierarchical clustering.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class OPTICSToHierarchicalTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testOPTICSToHierarchicalHeap() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, OPTICSToHierarchical.class) //
        .with(OPTICSToHierarchical.Par.OPTICS_ID, OPTICSHeap.class) //
        .with(AbstractOPTICS.Par.MINPTS_ID, 20) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.6860268);
    assertClusterSizes(clustering, new int[] { 1, 199, 438 });
  }

  @Test
  public void testOPTICSToHierarchicalList() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(CutDendrogramByNumberOfClusters.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .with(Algorithm.Utils.ALGORITHM_ID, OPTICSToHierarchical.class) //
        .with(OPTICSToHierarchical.Par.OPTICS_ID, OPTICSList.class) //
        .with(AbstractOPTICS.Par.MINPTS_ID, 20) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.6860268);
    assertClusterSizes(clustering, new int[] { 1, 199, 438 });
  }
}
