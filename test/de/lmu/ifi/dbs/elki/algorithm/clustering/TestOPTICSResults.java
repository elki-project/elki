package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full OPTICS run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that OPTICS's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestOPTICSResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run OPTICS with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testOPTICSResults() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(OPTICS.XI_ID, "0.038");
    params.addParameter(OPTICS.MINPTS_ID, 18);
    OPTICS<DoubleVector, DoubleDistance> optics = ClassGenericsUtil.parameterizeOrAbort(OPTICS.class, params);
    testParameterizationOk(params);

    // run OPTICS on database
    ClusterOrderResult<DoubleDistance> result = optics.run(db);
    Clustering<?> clustering = findSingleClustering(result);

    testFMeasure(db, clustering, 0.874062);
    testClusterSizes(clustering, new int[] { 109, 121, 210, 270 });
  }
}