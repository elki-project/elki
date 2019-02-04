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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.typeconversions.ClassLabelFilter;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full PreDeCon run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that PreDeCon performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class PreDeConTest extends AbstractClusterAlgorithmTest {
  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testPreDeConResults() {
    // Additional input parameters
    ListParameterization inp = new ListParameterization();
    inp.addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, ClassLabelFilter.class);
    inp.addParameter(ClassLabelFilter.Parameterizer.CLASS_LABEL_INDEX_ID, 1);
    Database db = makeSimpleDatabase(UNITTEST + "axis-parallel-subspace-clusters-6d.csv.gz", 2500, inp);

    Clustering<Model> result = new ELKIBuilder<PreDeCon<DoubleVector>>(PreDeCon.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 60) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 40) //
        .with(PreDeCon.Settings.Parameterizer.DELTA_ID, 400) //
        .with(PreDeCon.Settings.Parameterizer.KAPPA_ID, 20.) //
        .with(PreDeCon.Settings.Parameterizer.LAMBDA_ID, 4) //
        .build().run(db);

    // FIXME: find better working parameters?
    testFMeasure(db, result, 0.724752);
    testClusterSizes(result, new int[] { 43, 93, 108, 611, 638, 1007 });
  }

  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden
   * standard.O
   */
  @Test
  public void testPreDeConSubspaceOverlapping() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
    Clustering<Model> result = new ELKIBuilder<PreDeCon<DoubleVector>>(PreDeCon.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.3) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 10) //
        .with(PreDeCon.Settings.Parameterizer.DELTA_ID, 0.012) //
        .with(PreDeCon.Settings.Parameterizer.KAPPA_ID, 10.) //
        .with(PreDeCon.Settings.Parameterizer.LAMBDA_ID, 2) //
        .build().run(db);
    testFMeasure(db, result, 0.74982899);
    testClusterSizes(result, new int[] { 356, 494 });
  }
}
