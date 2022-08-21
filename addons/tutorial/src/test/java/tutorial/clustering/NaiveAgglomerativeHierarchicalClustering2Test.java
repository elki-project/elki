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
public class NaiveAgglomerativeHierarchicalClustering2Test extends AbstractClusterAlgorithmTest {
  @Test
  public void testSingleLink() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<NaiveAgglomerativeHierarchicalClustering2<?>>(NaiveAgglomerativeHierarchicalClustering2.class) //
        .with(CutDendrogramByNumberOfClusters.Par.MINCLUSTERS_ID, 3) //
        .build().run(db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD));
    assertClusterSizes(clustering, new int[] { 9, 200, 429 });
    assertFMeasure(db, clustering, 0.6829722);
  }
}
