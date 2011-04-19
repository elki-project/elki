package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.SNNClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SNNClustering run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that SNNClustering's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestSNNClusteringResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run SNNClustering with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSNNClusteringResults() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d.ascii", 1200);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(SNNClustering.EPSILON_ID, 77);
    params.addParameter(SNNClustering.MINPTS_ID, 28);
    params.addParameter(SharedNearestNeighborPreprocessor.Factory.NUMBER_OF_NEIGHBORS_ID, 100);
    SNNClustering<DoubleVector, DoubleDistance> snn = ClassGenericsUtil.parameterizeOrAbort(SNNClustering.class, params);
    testParameterizationOk(params);

    // run SNN on database
    Clustering<Model> result = snn.run(db);
    testFMeasure(db, result, 0.835000);
    testClusterSizes(result, new int[] { 76, 213, 219, 225, 231, 236 });
  }
}