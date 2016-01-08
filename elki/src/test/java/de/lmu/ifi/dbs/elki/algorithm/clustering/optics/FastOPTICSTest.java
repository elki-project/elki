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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.preprocessed.fastoptics.RandomProjectedNeighborsAndDensities;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Simple regression test for FastOPTICS.
 *
 * @author Erich Schubert
 */
public class FastOPTICSTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testFastOPTICS() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(OPTICSList.Parameterizer.MINPTS_ID, 18);
    params.addParameter(OPTICSXi.Parameterizer.XI_ID, 0.038);
    params.addParameter(OPTICSXi.Parameterizer.XIALG_ID, FastOPTICS.class);
    params.addParameter(RandomProjectedNeighborsAndDensities.Parameterizer.RANDOM_ID, 0);
    OPTICSXi opticsxi = ClassGenericsUtil.parameterizeOrAbort(OPTICSXi.class, params);
    testParameterizationOk(params);

    // run OPTICS on database
    Clustering<?> clustering = opticsxi.run(db);

    testFMeasure(db, clustering, 0.7495221);
    testClusterSizes(clustering, new int[] { 3, 4, 5, 6, 7, 8, 13, 18, 23, 26, 36, 40, 42, 95, 167, 217 });
  }
}