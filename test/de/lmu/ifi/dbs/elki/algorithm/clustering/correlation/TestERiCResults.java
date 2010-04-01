package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.RelativeEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Perform a full ERiC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that ERiC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 * 
 */
public class TestERiCResults implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testERiCResults() throws ParameterException {
    ListParameterization params = new ListParameterization();
    // Input
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    // ERiC
    params.addParameter(COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class);
    params.addParameter(DBSCAN.MINPTS_ID, 30);
    params.addParameter(DBSCAN.EPSILON_ID, 0);
    // ERiC Distance function in DBSCAN:
    params.addParameter(COPAC.PARTITION_DISTANCE_ID, ERiCDistanceFunction.class);
    params.addParameter(ERiCDistanceFunction.DELTA_ID, 0.20);
    params.addParameter(ERiCDistanceFunction.TAU_ID, 0.04);
    // Preprocessing via Local PCA:
    params.addParameter(COPAC.PREPROCESSOR_ID, KnnQueryBasedLocalPCAPreprocessor.class);
    params.addParameter(KnnQueryBasedLocalPCAPreprocessor.K_ID, 50);
    //params.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
    // PCA
    params.addParameter(PCARunner.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class);
    params.addParameter(RelativeEigenPairFilter.EIGENPAIR_FILTER_RALPHA, 1.60);
    
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    ERiC<DoubleVector> eric = new ERiC<DoubleVector>(params);
    eric.setVerbose(false);
    
    params.failOnErrors();
    if (params.hasUnusedParameters()) {
      for (Pair<OptionID,Object> pair : params.getRemainingParameters()) {
        LoggingUtil.warning("Unused: " + pair.first.getName() + " : " + pair.second.toString());
      }
      fail("Unused parameters.");
    }
    // run ERiC on database
    Clustering<CorrelationModel<DoubleVector>> result = eric.run(db);

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    // Even with not optimized parameters, we easily achieved 0.62
    // So any loss of quality means something isn't quite right with our
    // algorithms.
    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("ERiC score on test dataset too low: " + score, score > 0.920);
    System.out.println("ERiC score: " + score + " > " + 0.920);
  }
}