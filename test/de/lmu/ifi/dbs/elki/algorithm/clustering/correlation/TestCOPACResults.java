package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full COPAC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that COPAC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * @author Katharina Rausch
 */
public class TestCOPACResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run COPAC with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCOPACResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);

    // these parameters are not picked too smartly - room for improvement.
    ListParameterization params = new ListParameterization();
    params.addParameter(COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class);
    params.addParameter(DBSCAN.EPSILON_ID, 0.02);
    params.addParameter(DBSCAN.MINPTS_ID, 50);
    params.addParameter(COPAC.PREPROCESSOR_ID, KNNQueryFilteredPCAIndex.Factory.class);
    params.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, 15);

    COPAC<DoubleVector, DoubleDistance> copac = ClassGenericsUtil.parameterizeOrAbort(COPAC.class, params);
    testParameterizationOk(params);

    // run COPAC on database
    Clustering<Model> result = copac.run(db);

    testFMeasure(db, result, 0.842521);
    testClusterSizes(result, new int[] { 6, 16, 32, 196, 200 });
  }

  /**
   * Run COPAC with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCOPACOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
  
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class);
    params.addParameter(DBSCAN.EPSILON_ID, 0.5);
    params.addParameter(DBSCAN.MINPTS_ID, 20);
    params.addParameter(COPAC.PREPROCESSOR_ID, KNNQueryFilteredPCAIndex.Factory.class);
    params.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, 45);
    // PCA
    params.addParameter(PCARunner.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class);
    params.addParameter(PercentageEigenPairFilter.ALPHA_ID, 0.8);
  
    COPAC<DoubleVector, DoubleDistance> copac = ClassGenericsUtil.parameterizeOrAbort(COPAC.class, params);
    testParameterizationOk(params);
  
    Clustering<Model> result = copac.run(db);
    testFMeasure(db, result, 0.84687864);
    testClusterSizes(result, new int[] { 1, 22, 22, 29, 34, 158, 182, 202 });
  }
}