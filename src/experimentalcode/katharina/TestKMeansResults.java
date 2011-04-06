package experimentalcode.katharina;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.KMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full KMeans run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that KMeans's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestKMeansResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "src/experimentalcode/katharina/katharina/1dbscan_failure_without_noise.ascii";

  // size of the data set
  int shoulds = 1000;

  /**
   * Run KMeans with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testKMeansResults() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(dataset, shoulds);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.K_ID, "5");
    params.addParameter(KMeans.SEED_ID, "3");
    KMeans<DoubleVector, DoubleDistance> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeans.class, params);
    testParameterizationOk(params);

    // run KMeans on database
    Clustering<MeanModel<DoubleVector>> result = kmeans.run(db);
    testFMeasure(db, result, 0.998005);
    testClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }
}