package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.SLINK;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full SLINK run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that SLINK's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestSLINKResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  // TODO: add a test for a non-single-link dataset?
  
  /**
   * Run SLINK with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testSLINKResults() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(SLINK.SLINK_MINCLUSTERS_ID, 3);
    SLINK<DoubleVector, DoubleDistance> slink = ClassGenericsUtil.parameterizeOrAbort(SLINK.class, params);
    testParameterizationOk(params);

    // run SLINK on database
    Result result = slink.run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.6829722);
    testClusterSizes(clustering, new int[] { 0, 0, 9, 200, 429 });
  }
}