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
package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(DeLiClu.Parameterizer.MINPTS_ID, 18) //
        .with(OPTICSXi.Parameterizer.XI_ID, 0.038) //
        .with(OPTICSXi.Parameterizer.XIALG_ID, DeLiClu.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 1000) //
        .build().run(db);
    // Test F-Measure
    Clustering<Model> rbl = new ByLabelClustering().run(db);
    ClusterContingencyTable ct = new ClusterContingencyTable(true, false);
    ct.process(clustering, rbl);
    double score = ct.getPaircount().f1Measure();
    // We cannot test exactly - due to Hashing, DeLiClu sequence is not
    // identical each time, the results will vary.
    if(Math.abs(score - 0.8771174) < 1e-5) {
      assertEquals("Score does not match.", 0.8771174, score, 1e-5);
    }
    else {
      assertEquals("Score does not match.", 0.8819664, score, 1e-5);
    }
  }
}
