package experimentalcode.katharina;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.KMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
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
 * 
 */
public class TestKMeansResults implements JUnit4Test {
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
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);
    params.addParameter(KMeans.K_ID, "5");
    params.addParameter(KMeans.SEED_ID, "3");

    FileBasedDatabaseConnection<DoubleVector> dbconn = FileBasedDatabaseConnection.parameterize(params);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertEquals("Database size doesn't match expected size.", shoulds, db.size());

    // setup algorithm
    KMeans<DoubleVector, DoubleDistance> kmeans = ClassGenericsUtil.parameterizeOrAbort(KMeans.class, params);

    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      fail("Unused parameters: " + params.getRemainingParameters());
    }
    // run KMeans on database
    Clustering<MeanModel<DoubleVector>> result = kmeans.run(db);

    // run by-label as reference
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    assertTrue("KMeans score on test dataset too low: " + score, score > 0.997);
    System.out.println("KMeans score: " + score + " > " + 0.997);
  }
}