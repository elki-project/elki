package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.datasource.filter.typeconversions.ClassLabelFilter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full PreDeCon run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that PreDeCon performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * @author Katharina Rausch
 */
public class TestPreDeConResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPreDeConResults() {
    // Additional input parameters
    ListParameterization inp = new ListParameterization();
    inp.addParameter(ClassLabelFilter.Parameterizer.CLASS_LABEL_INDEX_ID, 1);
    Class<?>[] filters = new Class<?>[] { ClassLabelFilter.class };
    Database db = makeSimpleDatabase(UNITTEST + "axis-parallel-subspace-clusters-6d.csv.gz", 2500, inp, filters);

    ListParameterization params = new ListParameterization();
    // PreDeCon
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 60);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 40);
    params.addParameter(PreDeCon.Settings.Parameterizer.DELTA_ID, 400);
    params.addParameter(PreDeCon.Settings.Parameterizer.KAPPA_ID, 20.);
    params.addParameter(PreDeCon.Settings.Parameterizer.LAMBDA_ID, 4);

    // setup algorithm
    PreDeCon<DoubleVector> predecon = ClassGenericsUtil.parameterizeOrAbort(PreDeCon.class, params);
    testParameterizationOk(params);

    // run PredeCon on database
    Clustering<Model> result = predecon.run(db);

    // FIXME: find working parameters...
    testFMeasure(db, result, 0.724752);
    testClusterSizes(result, new int[] { 43, 93, 108, 611, 638, 1007 });
  }

  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPreDeConSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    // PreDeCon
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.3);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 10);
    params.addParameter(PreDeCon.Settings.Parameterizer.DELTA_ID, 0.012);
    params.addParameter(PreDeCon.Settings.Parameterizer.KAPPA_ID, 10.);
    params.addParameter(PreDeCon.Settings.Parameterizer.LAMBDA_ID, 2);
    PreDeCon<DoubleVector> predecon = ClassGenericsUtil.parameterizeOrAbort(PreDeCon.class, params);
    testParameterizationOk(params);

    // run PredeCon on database
    Clustering<Model> result = predecon.run(db);
    testFMeasure(db, result, 0.74982899);
    testClusterSizes(result, new int[] { 356, 494 });
  }
}
