package de.lmu.ifi.dbs.elki.algorithm.outlier;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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

  static int k = 5;

  static int size = 50;

  static int seed = 5;

  @Test
  public void testOnlineLOF() throws UnableToComplyException {
    Database<DoubleVector> db = getDatabase();

    // run first LOF, then OnlineLOF (with delete and insert) on database
    OutlierResult result1 = runLOF(db);
    OutlierResult result2 = runOnlineLOF(db);

    AnnotationResult<Double> scores1 = result1.getScores();
    AnnotationResult<Double> scores2 = result2.getScores();

    for(DBID id : db.getIDs()) {
      Double lof1 = scores1.getValueFor(id);
      Double lof2 = scores2.getValueFor(id);
      assertTrue("lof(" + id + ") != lof(" + id + "): " + lof1 + " != " + lof2, lof1.equals(lof2));
    }
  }

  private static ListParameterization lofParameter() {
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, k);
    params.addParameter(LOF.REACHABILITY_DISTANCE_FUNCTION_ID, CosineDistanceFunction.class.getName());
    return params;
  }

  private static OutlierResult runLOF(Database<DoubleVector> db) {
    // setup algorithm
    ListParameterization params = lofParameter();
    LOF<DoubleVector, DoubleDistance> lof = null;
    Class<LOF<DoubleVector, DoubleDistance>> lofcls = ClassGenericsUtil.uglyCastIntoSubclass(LOF.class);
    lof = params.tryInstantiate(lofcls, lofcls);
    params.failOnErrors();

    // run LOF on database
    return lof.run(db);
  }

  private static OutlierResult runOnlineLOF(Database<DoubleVector> db) throws UnableToComplyException {
    // setup algorithm
    ListParameterization params = lofParameter();
    OnlineLOF<DoubleVector, DoubleDistance> lof = null;
    Class<OnlineLOF<DoubleVector, DoubleDistance>> lofcls = ClassGenericsUtil.uglyCastIntoSubclass(OnlineLOF.class);
    lof = params.tryInstantiate(lofcls, lofcls);
    params.failOnErrors();

    // run OnlineLOF on database
    OutlierResult result = lof.run(db);

    // insert new objects
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    DoubleVector o = db.get(db.getIDs().iterator().next());
    Random random = new Random(seed);
    for(int i = 0; i < size; i++) {
      DoubleVector obj = o.randomInstance(random);
      insertions.add(new Pair<DoubleVector, DatabaseObjectMetadata>(obj, new DatabaseObjectMetadata()));
    }
    DBIDs deletions = db.insert(insertions);

    // delete objects
    db.delete(deletions);

    return result;
  }

  private static Database<DoubleVector> getDatabase() {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      fail("Unused parameters: " + params.getRemainingParameters());
    }

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);
    return db;
  }

}
