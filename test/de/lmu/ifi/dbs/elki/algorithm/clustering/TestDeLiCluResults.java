package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTreeFactory;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full DeLiClu run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that DeLiClu's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestDeLiCluResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run DeLiClu with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDeLiCluResults() throws ParameterException {
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, UNITTEST + "hierarchical-2d.ascii");
    params.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);
    params.addParameter(HashmapDatabase.INDEX_ID, DeLiCluTreeFactory.class);
    params.addParameter(DeLiCluTreeFactory.PAGE_SIZE_ID, 1000);
    params.addParameter(DeLiClu.XI_ID, 0.038);
    params.addParameter(DeLiClu.MINPTS_ID, 18);
    FileBasedDatabaseConnection<DoubleVector> dbconn = ClassGenericsUtil.parameterizeOrAbort(FileBasedDatabaseConnection.class, params);
    Database<DoubleVector> db = dbconn.getDatabase(null);
    org.junit.Assert.assertEquals("Database size does not match.", 710, db.size());

    DeLiClu<DoubleVector, DoubleDistance> deliclu = ClassGenericsUtil.parameterizeOrAbort(DeLiClu.class, params);
    testParameterizationOk(params);

    // run DeLiClu on database
    ClusterOrderResult<DoubleDistance> result = deliclu.run(db);
    Clustering<?> clustering = findSingleClustering(result);
    testFMeasure(db, clustering, 0.87406257);
    testClusterSizes(clustering, new int[] { 109, 121, 210, 270 });
  }
}