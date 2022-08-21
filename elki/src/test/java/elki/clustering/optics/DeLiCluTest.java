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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.trivial.ByLabelClustering;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.Database;
import elki.evaluation.clustering.ClusterContingencyTable;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full DeLiClu run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that DeLiClu's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.7.0
 */
public class DeLiCluTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DeLiClu with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testDeLiCluResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    Clustering<?> clustering = new ELKIBuilder<>(OPTICSXi.class) //
        .with(DeLiClu.Par.MINPTS_ID, 20) //
        .with(OPTICSXi.Par.XI_ID, 0.05) //
        .with(OPTICSXi.Par.XIALG_ID, DeLiClu.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 1000) //
        .build().autorun(db);
    // Test F-Measure
    Clustering<Model> rbl = new ByLabelClustering().autorun(db);
    double score = new ClusterContingencyTable(true, false, clustering, rbl).getPaircount().f1Measure();
    // We cannot test exactly - due to Hashing, DeLiClu sequence is not
    // identical each time, the results will vary.
    assertEquals("Score does not match.", 0.891033, score, 1e-5);
  }
}
