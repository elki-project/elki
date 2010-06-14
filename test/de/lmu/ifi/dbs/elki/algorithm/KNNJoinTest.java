package de.lmu.ifi.dbs.elki.algorithm;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNList;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

public class KNNJoinTest implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/uebungsblatt-2d-mini.csv";

  // size of the data set
  int shoulds = 20;

  // mean number of 2NN
  double mean2nnEuclid = 2.85;

  // variance
  double var2nnEuclid = 1.2985526315;

  /**
   * Test {@link RStarTree} using a file based database connection.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testKNNJoinRtreeMini() throws ParameterException {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    spatparams.addParameter(SpatialIndexDatabase.INDEX_ID, RStarTree.class);
    spatparams.addParameter(TreeIndex.PAGE_SIZE_ID, 200);

    doKNNJoin(spatparams);
  }

  /**
   * Test {@link RStarTree} using a file based database connection.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testKNNJoinRtreeMaxi() throws ParameterException {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    spatparams.addParameter(SpatialIndexDatabase.INDEX_ID, RStarTree.class);
    spatparams.addParameter(TreeIndex.PAGE_SIZE_ID, 2000);

    doKNNJoin(spatparams);
  }

  /**
   * Test {@link DeLiCluTree} using a file based database connection.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testKNNJoinDeLiCluTreeMini() throws ParameterException {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    spatparams.addParameter(SpatialIndexDatabase.INDEX_ID, DeLiCluTree.class);
    spatparams.addParameter(TreeIndex.PAGE_SIZE_ID, 200);

    doKNNJoin(spatparams);
  }

  /**
   * Actual test routine.
   * 
   * @param inputparams
   * @throws ParameterException
   */
  void doKNNJoin(ListParameterization inputparams) throws ParameterException {
    inputparams.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    inputparams.addParameter(FileBasedDatabaseConnection.IDSTART_ID, 1);

    // get database
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(inputparams);
    Database<DoubleVector> db = dbconn.getDatabase(null);
    inputparams.failOnErrors();

    // verify data set size.
    org.junit.Assert.assertEquals("Database size does not match.", shoulds, db.size());

    // Euclidean
    {
      ListParameterization knnparams = new ListParameterization();
      knnparams.addParameter(KNNJoin.K_ID, 2);
      KNNJoin<DoubleVector, DoubleDistance, ?, ?> knnjoin = new KNNJoin<DoubleVector, DoubleDistance, RStarTreeNode, SpatialEntry>(knnparams);
      AnnotationFromHashMap<KNNList<DoubleDistance>> result = knnjoin.run(db);

      MeanVariance meansize = new MeanVariance();
      for(DBID id : db) {
        KNNList<DoubleDistance> knnlist = result.getValueFor(id);
        meansize.put(knnlist.size());
      }
      org.junit.Assert.assertEquals("Euclidean mean 2NN", mean2nnEuclid, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Euclidean variance 2NN", var2nnEuclid, meansize.getVariance(), 0.00001);
    }
    // Manhattan
    // TODO: Add Manhattan support.
    /*{
      ListParameterization knnparams = new ListParameterization();
      knnparams.addParameter(KNNJoin.K_ID, 2);
      knnparams.addParameter(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, ManhattanDistanceFunction.class);
      KNNJoin<DoubleVector, DoubleDistance, ?, ?> knnjoin = new KNNJoin<DoubleVector, DoubleDistance, RStarTreeNode, SpatialEntry>(knnparams);
      AnnotationFromHashMap<KNNList<DoubleDistance>> result = knnjoin.run(db);

      MeanVariance meansize = new MeanVariance();
      for(DBID id : db) {
        KNNList<DoubleDistance> knnlist = result.getValueFor(id);
        meansize.put(knnlist.size());
      }
      System.err.println("" + meansize);
      org.junit.Assert.assertEquals("Manhattan mean 2NN", mean2nnManhattan, meansize.getMean(), 0.00001);
      org.junit.Assert.assertEquals("Manhattan variance 2NN", var2nnManhattan, meansize.getVariance(), 0.00001);
    }*/
  }
}
