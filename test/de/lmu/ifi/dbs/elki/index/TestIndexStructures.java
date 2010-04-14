package de.lmu.ifi.dbs.elki.index;

import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.MetricalIndexDatabase;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree.MTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTree;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test case to validate some index structures for accuracy. For a known data
 * set and query point, the top 10 nearest neighbors are queried and verified.
 * 
 * Note that the internal operation of the index structure is not tested this
 * way, only whether the database object with the index still returns reasonable
 * results.
 * 
 * @author Erich Schubert
 */
public class TestIndexStructures implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  // query point
  double[] querypoint = new double[] { 0.5, 0.5, 0.5 };

  // number of kNN to query
  int k = 10;

  // the 10 next neighbors of the query point
  double[][] shouldc = new double[][] { { 0.45000428746088883, 0.484504234161508, 0.5538595167151342 }, { 0.4111050036231091, 0.429204794352013, 0.4689430202460606 }, { 0.4758477631164003, 0.6021538103067177, 0.5556807408692025 }, { 0.4163288957164025, 0.49604545242979536, 0.4054361013566713 }, { 0.5819940640461848, 0.48586944418231115, 0.6289592025558619 }, { 0.4373568207802466, 0.3468650110814596, 0.49566951629699485 }, { 0.40283109564192643, 0.6301433694690401, 0.44313571161129883 }, { 0.6545840114867083, 0.4919617658889418, 0.5905461546078652 }, { 0.6011097673869055, 0.6562921241634017, 0.44830647520493694 }, { 0.5127485678175534, 0.29708449200895504, 0.561722374659424 }, };

  // and their distances
  double[] shouldd = new double[] { 0.07510351238126374, 0.11780839322826206, 0.11882371989803064, 0.1263282354232315, 0.15347043712184602, 0.1655090505771259, 0.17208323533934652, 0.17933052146586306, 0.19319066655063877, 0.21247795391113142 };

  /**
   * Test exact query, also to validate the test is correct.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testExcat() throws ParameterException {
    ListParameterization params = new ListParameterization();
    testFileBasedDatabaseConnection(params);
  }

  /**
   * Test {@link MTree} using a file based database connection.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testMetrical() throws ParameterException {
    ListParameterization metparams = new ListParameterization();
    metparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, MetricalIndexDatabase.class);
    metparams.addParameter(MetricalIndexDatabase.INDEX_ID, MTree.class);
    testFileBasedDatabaseConnection(metparams);
  }

  /**
   * Test {@link RStarTree} using a file based database connection.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testRStarTree() throws ParameterException {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    spatparams.addParameter(SpatialIndexDatabase.INDEX_ID, RStarTree.class);
    testFileBasedDatabaseConnection(spatparams);
  }

  /**
   * Test {@link RStarTree} using a file based database connection.
   * With "fast" mode enabled on an extreme level (since this should only reduce performance,
   * not accuracy!)
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testRStarTreeFast() throws ParameterException {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    spatparams.addParameter(SpatialIndexDatabase.INDEX_ID, RStarTree.class);
    spatparams.addParameter(AbstractRStarTree.INSERTION_CANDIDATES_ID, 1);
    testFileBasedDatabaseConnection(spatparams);
  }

  /**
   * Test {@link XTree} using a file based database connection.
   * 
   * @throws ParameterException
   */
  /*@Test
  public void testXTree() throws ParameterException {
    ListParameterization xtreeparams = new ListParameterization();
    xtreeparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    xtreeparams.addParameter(SpatialIndexDatabase.INDEX_ID, XTree.class);
    testFileBasedDatabaseConnection(xtreeparams);
  }*/

  /**
   * Actual test routine.
   * 
   * @param inputparams
   * @throws ParameterException
   */
  void testFileBasedDatabaseConnection(ListParameterization inputparams) throws ParameterException {
    inputparams.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    // get database
    DistanceFunction<DoubleVector, DoubleDistance> dist = new EuclideanDistanceFunction<DoubleVector>();
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(inputparams);
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertTrue(db.size() == shoulds);

    // get the 10 next neighbors
    DoubleVector dv = new DoubleVector(querypoint);
    List<DistanceResultPair<DoubleDistance>> ids = db.kNNQueryForObject(dv, k, dist);

    // verify that the neighbors match.
    int i = 0;
    for(DistanceResultPair<DoubleDistance> res : ids) {
      int id = res.getID();
      DoubleVector c = db.get(id);
      // verify vector
      DoubleVector c2 = new DoubleVector(shouldc[i]);
      assertTrue(dist.distance(c, c2).doubleValue() < 0.00001);

      // Verify distance
      assertTrue(res.getDistance().doubleValue() == shouldd[i]);
      i++;
    }
  }
}