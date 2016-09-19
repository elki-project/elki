package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Regression test for simplified hierarchy extraction.
 *
 * @author Erich Schubert
 */
public class SimplifiedHierarchyExtractionTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testSLINKResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(SimplifiedHierarchyExtraction.Parameterizer.MINCLUSTERSIZE_ID, 50);
    params.addParameter(AlgorithmStep.Parameterizer.ALGORITHM_ID, SLINK.class);
    SimplifiedHierarchyExtraction slink = ClassGenericsUtil.parameterizeOrAbort(SimplifiedHierarchyExtraction.class, params);
    testParameterizationOk(params);

    // run SLINK on database
    Result result = slink.run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.696491);
    testClusterSizes(clustering, new int[] { 3, 5, 43, 55, 58, 62, 104 });
  }
}
