package de.lmu.ifi.dbs.elki.test.algorithm.clustering.correlation;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.varianceanalysis.RelativeEigenPairFilter;
import de.lmu.ifi.dbs.elki.varianceanalysis.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.ErfcWeight;

public class TestERiCResults {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  @Test
  public void testERiCResults() throws ParameterException {
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>();

    String[] inputparams = new String[0];
    // Set up database input file:
    inputparams = Util.addParameter(inputparams, FileBasedDatabaseConnection.INPUT_ID, dataset);
    inputparams = dbconn.setParameters(inputparams);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertTrue(db.size() == shoulds);

    // setup algorithm
    ERiC<DoubleVector> eric = new ERiC<DoubleVector>();

    // prepare parameters
    List<String> ericparams = new ArrayList<String>();
    eric.setVerbose(false);
    Util.addParameter(ericparams, COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class.getCanonicalName());
    Util.addParameter(ericparams, DBSCAN.MINPTS_ID, Integer.toString(30));
    Util.addParameter(ericparams, DBSCAN.EPSILON_ID, Integer.toString(0));
    // ERiC Distance function in DBSCAN:
    Util.addParameter(ericparams, DBSCAN.DISTANCE_FUNCTION_ID, ERiCDistanceFunction.class.getCanonicalName());
    Util.addParameter(ericparams, ERiCDistanceFunction.DELTA_ID, Double.toString(0.20));
    Util.addParameter(ericparams, ERiCDistanceFunction.TAU_ID, Double.toString(0.04));
    // Preprocessing via HiCo:
    Util.addParameter(ericparams, COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getCanonicalName());
    Util.addParameter(ericparams, KnnQueryBasedHiCOPreprocessor.KNN_HICO_PREPROCESSOR_K, Integer.toString(50));
    Util.addFlag(ericparams, PreprocessorHandler.OMIT_PREPROCESSING_ID);
    // PCA
    Util.addParameter(ericparams, PCAFilteredRunner.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class.getCanonicalName());
    Util.addParameter(ericparams, WeightedCovarianceMatrixBuilder.WEIGHT_ID, ErfcWeight.class.getCanonicalName());
    Util.addParameter(ericparams, PCAFilteredRunner.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class.getCanonicalName());
    Util.addParameter(ericparams, RelativeEigenPairFilter.EIGENPAIR_FILTER_RALPHA, Double.toString(1.60));
    // Set parameters
    String[] remainingparams = eric.setParameters(ericparams.toArray(new String[0]));
    for(String s : remainingparams)
      System.err.println("Remaining parameter: " + s);
    assertTrue("Some parameters were ignored by the algorithm.", remainingparams.length == 0);
    // run ERiC on database
    eric.run(db);
    Clustering<Cluster<CorrelationModel<DoubleVector>>> result = eric.getResult();

    // run by-label as reference
    ByLabelHierarchicalClustering<DoubleVector> bylabel = new ByLabelHierarchicalClustering<DoubleVector>();
    bylabel.run(db);
    Clustering<Cluster<Model>> rbl = bylabel.getResult();

    // Even with not optimized parameters, we easily achieved 0.62
    // So any loss of quality means something isn't quite right with our
    // algorithms.
    double score = PairCountingFMeasure.compareDatabases(result, rbl, 1.0);
    assertTrue("ERiC score on test dataset too low: " + score, score > 0.920);
    System.out.println("ERiC score: " + score + " > " + 0.920);
  }

}
