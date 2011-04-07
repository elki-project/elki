package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.RelativeEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full ERiC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that ERiC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 */
public class TestERiCResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testERiCResults() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);

    // ERiC
    ListParameterization params = new ListParameterization();
    params.addParameter(COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class);
    params.addParameter(DBSCAN.MINPTS_ID, 30);
    params.addParameter(DBSCAN.EPSILON_ID, 0);
    // ERiC Distance function in DBSCAN:
    params.addParameter(COPAC.PARTITION_DISTANCE_ID, ERiCDistanceFunction.class);
    params.addParameter(ERiCDistanceFunction.DELTA_ID, 0.20);
    params.addParameter(ERiCDistanceFunction.TAU_ID, 0.04);
    // Preprocessing via Local PCA:
    params.addParameter(COPAC.PREPROCESSOR_ID, KNNQueryFilteredPCAIndex.Factory.class);
    params.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, 50);
    // PCA
    params.addParameter(PCARunner.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class);
    params.addParameter(RelativeEigenPairFilter.EIGENPAIR_FILTER_RALPHA, 1.60);

    ERiC<DoubleVector> eric = ClassGenericsUtil.parameterizeOrAbort(ERiC.class, params);
    testParameterizationOk(params);

    // run ERiC on database
    Clustering<CorrelationModel<DoubleVector>> result = eric.run(db);

    testFMeasureHierarchical(db, result, 0.9204825);
    testClusterSizes(result, new int[] { 109, 184, 307 });
  }
}