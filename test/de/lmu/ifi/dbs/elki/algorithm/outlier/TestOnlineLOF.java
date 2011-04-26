package de.lmu.ifi.dbs.elki.algorithm.outlier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the OnlineLOF algorithm. Compares the result of the static LOF
 * algorithm to the result of the OnlineLOF algorithm, where some insertions and
 * deletions (of the previously inserted objects) have been applied to the
 * database.
 * 
 * @author Elke Achtert
 * 
 */
public class TestOnlineLOF implements JUnit4Test {
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";

  // parameter k for LOF and OnlineLOF
  static int k = 5;

  // neighborhood distance function for LOF and OnlineLOF
  @SuppressWarnings("rawtypes")
  static DistanceFunction neighborhoodDistanceFunction = EuclideanDistanceFunction.STATIC;

  // reachability distance function for LOF and OnlineLOF
  @SuppressWarnings("rawtypes")
  static DistanceFunction reachabilityDistanceFunction = new CosineDistanceFunction();

  // seed for the generator
  static int seed = 5;

  // size of the data set
  static int size = 50;

  /**
   * First, run the {@link LOF} algorithm on the database. Second, run the
   * {@link OnlineLOF} algorithm on the database, insert new objects and
   * afterwards delete them. Then, compare the two results for equality.
   * 
   * @throws UnableToComplyException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testOnlineLOF() throws UnableToComplyException {
    Database db = getDatabase();

    // 1. Run LOF
    LOF<DoubleVector, DoubleDistance> lof = new LOF<DoubleVector, DoubleDistance>(k, neighborhoodDistanceFunction, reachabilityDistanceFunction);
    OutlierResult result1 = lof.run(db);

    // 2. Run OnlineLOF (with insertions and removals) on database
    OutlierResult result2 = runOnlineLOF(db);

    // 3. Compare results
    AnnotationResult<Double> scores1 = result1.getScores();
    AnnotationResult<Double> scores2 = result2.getScores();

    for(DBID id : db.getDBIDs()) {
      Double lof1 = scores1.getValueFor(id);
      Double lof2 = scores2.getValueFor(id);
      assertTrue("lof(" + id + ") != lof(" + id + "): " + lof1 + " != " + lof2, lof1.equals(lof2));
    }
  }

  /**
   * Run OnlineLOF (with insertions and removals) on database.
   */
  @SuppressWarnings("unchecked")
  private static OutlierResult runOnlineLOF(Database db) throws UnableToComplyException {
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

    // setup algorithm
    OnlineLOF<DoubleVector, DoubleDistance> lof = new OnlineLOF<DoubleVector, DoubleDistance>(k, neighborhoodDistanceFunction, reachabilityDistanceFunction);
    
    // run OnlineLOF on database
    OutlierResult result = lof.run(db);

    // insert new objects
    ArrayList<Object> insertions = new ArrayList<Object>();
    DoubleVector o = DatabaseUtil.assumeVectorField(rep).getFactory();
    Random random = new Random(seed);
    for(int i = 0; i < size; i++) {
      DoubleVector obj = o.randomInstance(random);
      insertions.add(obj);
    }
    BundleMeta meta = new BundleMeta();
    meta.add(rep.getDataTypeInformation());
    List<List<?>> columns = new ArrayList<List<?>>(1);
    columns.add(insertions);
    DBIDs deletions = db.insert(new MultipleObjectsBundle(meta , columns));

    // delete objects
    db.delete(deletions);

    return result;
  }

  /**
   * Returns the database.
   */
  private static Database getDatabase() {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    FileBasedDatabaseConnection dbconn = ClassGenericsUtil.parameterizeOrAbort(FileBasedDatabaseConnection.class, params);
    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      fail("Unused parameters: " + params.getRemainingParameters());
    }

    // get database
    Database db = dbconn.getDatabase();
    return db;
  }

}
