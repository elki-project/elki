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
package elki.clustering.optics;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full OPTICS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that OPTICS's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class OPTICSHeapTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testOPTICS() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> clustering = new ELKIBuilder<>(OPTICSXi.class) //
        .with(OPTICSHeap.Par.MINPTS_ID, 20) //
        .with(OPTICSHeap.Par.EPSILON_ID, 0.15) //
        .with(OPTICSXi.Par.XI_ID, 0.05) //
        .with(OPTICSXi.Par.XIALG_ID, OPTICSHeap.class) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.893865);
    assertClusterSizes(clustering, new int[] { 8, 35, 72, 115, 209, 271 });
  }
}
