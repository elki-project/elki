package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTreeFactory;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full DeLiClu run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that DeLiClu's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.4.0
 */
public class DeLiCluTest extends AbstractSimpleAlgorithmTest {
  /**
   * Run DeLiClu with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDeLiCluResults() {
    ListParameterization indexparams = new ListParameterization();
    // We need a special index for this algorithm:
    indexparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, DeLiCluTreeFactory.class);
    indexparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 1000);
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710, indexparams, null);

    // Setup actual algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DeLiClu.Parameterizer.MINPTS_ID, 18);
    params.addParameter(OPTICSXi.Parameterizer.XI_ID, 0.038);
    params.addParameter(OPTICSXi.Parameterizer.XIALG_ID, DeLiClu.class);
    OPTICSXi opticsxi = ClassGenericsUtil.parameterizeOrAbort(OPTICSXi.class, params);
    testParameterizationOk(params);

    // run DeLiClu on database
    Clustering<?> clustering = opticsxi.run(db);

    // Test F-Measure
    ByLabelClustering bylabel = new ByLabelClustering();
    Clustering<Model> rbl = bylabel.run(db);
    ClusterContingencyTable ct = new ClusterContingencyTable(true, false);
    ct.process(clustering, rbl);
    double score = ct.getPaircount().f1Measure();
    // We cannot test exactly - due to Hashing, DeLiClu sequence is not
    // identical each time, the results will vary.
    if(Math.abs(score - 0.8771174) < 1e-5) {
      assertEquals("Score does not match.", 0.8771174, score, 1E-5);
    }
    else {
      assertEquals("Score does not match.", 0.8109293, score, 1E-5);
    }
  }
}