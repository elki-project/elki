package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.WithinClusterVarianceQualityMeasure;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the KMeansBisecting
 * 
 * @author Stephan Baier
 */
public class TestKMeansBisecting extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run KMeansBisecting with fixed parameters and compare cluster size to
   * expected value.
   */
  @Test
  public void testKMeansBisectingClusterSize() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, 3);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, KMeansLloyd.class);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVarianceQualityMeasure.class);

    KMeansBisecting<DoubleVector, DoubleDistance, MeanModel<DoubleVector>> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeansBisecting.class, params);
    testParameterizationOk(params);

    // run KMedians on database
    Clustering<MeanModel<DoubleVector>> result = kmeans.run(db);
    testClusterSizes(result, new int[] { 103, 97, 100 });
  }

  /**
   * Run KMeansBisecting with fixed parameters (k = 2) and compare f-measure to
   * golden standard.
   */
  @Test
  public void testKMeansBisectingFMeasure() {
    Database db = makeSimpleDatabase(UNITTEST + "bisecting-test.csv", 300);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, 2);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.TRIALS_ID, 5);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.KMEANS_ID, KMeansLloyd.class);
    params.addParameter(BestOfMultipleKMeans.Parameterizer.QUALITYMEASURE_ID, WithinClusterVarianceQualityMeasure.class);

    KMeansBisecting<DoubleVector, DoubleDistance, MeanModel<DoubleVector>> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeansBisecting.class, params);
    testParameterizationOk(params);

    // run KMedians on database
    Clustering<MeanModel<DoubleVector>> result = kmeans.run(db);
    testFMeasure(db, result, 0.7408);
  }
}
