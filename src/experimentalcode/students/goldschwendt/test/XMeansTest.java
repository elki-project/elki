package experimentalcode.students.goldschwendt.test;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.BayesianInformationCriterion;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import experimentalcode.students.goldschwendt.XMeans;

public class XMeansTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  
  @Test
  public void testKMeansLloyd() {
    Database db = makeSimpleDatabase("data/synthetic/outlier-scenarios/3-gaussian-2d.csv", 930);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(XMeans.Parameterizer.K_MIN_ID, 2);
    params.addParameter(XMeans.Parameterizer.K_MAX_ID, 20);
    params.addParameter(XMeans.Parameterizer.INITIAL_KMEANS_ID, KMeansLloyd.class);
    params.addParameter(XMeans.Parameterizer.SPLIT_KMEANS_ID, KMeansLloyd.class);
    params.addParameter(XMeans.Parameterizer.INFORMATION_CRITERION_ID, BayesianInformationCriterion.class);
    params.addParameter(KMeans.SEED_ID, 2);
    
    XMeans<DoubleVector, ?> xmeans = ClassGenericsUtil.parameterizeOrAbort(XMeans.class, params);
    testParameterizationOk(params);

    // run XMeans on database
    Clustering<?> result = xmeans.run(db);
    testFMeasure(db, result, 0.99119171244747);
    testClusterSizes(result, new int[] { 14, 303, 303, 9, 301 });
  }

}
