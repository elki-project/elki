package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

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
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Performs a full CLINK run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that CLINK's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 */
public class TestCLINKResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run CLINK with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testCLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(ExtractFlatClusteringFromHierarchy.Parameterizer.OUTPUTMODE_ID, ExtractFlatClusteringFromHierarchy.OutputMode.STRICT_PARTITIONS);
    params.addParameter(ExtractFlatClusteringFromHierarchy.Parameterizer.MINCLUSTERS_ID, 2);
    params.addParameter(AlgorithmStep.Parameterizer.ALGORITHM_ID, CLINK.class);
    ExtractFlatClusteringFromHierarchy slink = ClassGenericsUtil.parameterizeOrAbort(ExtractFlatClusteringFromHierarchy.class, params);
    testParameterizationOk(params);

    // run SLINK on database
    Result result = slink.run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.5147426);
    testClusterSizes(clustering, new int[] { 131, 507 });
  }
}
