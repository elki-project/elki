package experimentalcode.elke.algorithm.lof;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * 
 * @author Elke Achtert
 * 
 */
public class TestOnlineLOF {
  // the following values depend on the data set used!
  static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";

  static int k = 5;

  public static void main(String[] args) throws UnableToComplyException {
    Database<DoubleVector> db = getDatabase();
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, k);

    System.out.println(db.getIDs());

    // run first OnlineLOF (with delete and insert) on database, then run LOF
    OutlierResult result1 = runOnlineLOF(db);
    OutlierResult result2 = runLOF(db);

    AnnotationResult<Double> scores1 = result1.getScores();
    AnnotationResult<Double> scores2 = result2.getScores();

    for(DBID id : db.getIDs()) {
      Double lof1 = scores1.getValueFor(id);
      Double lof2 = scores2.getValueFor(id);

      if(lof1 == null || lof2 == null) {
        System.out.println("lof(" + id + ") != lof(" + id + "): " + lof1 + " != " + lof2);
      }

      else if(!lof1.equals(lof2)) {
        System.out.println("lof(" + id + ") != lof(" + id + "): " + lof1 + " != " + lof2);
      }
      // assertTrue("lof(" + id + ") != lof(" + id + "): " + lof1 + " != " +
      // lof2, lof1 == lof2);
    }

  }

  private static ListParameterization lofParameter() {
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, k);
    // params.addParameter(LOF.REACHABILITY_DISTANCE_FUNCTION_ID,
    // ManhattanDistanceFunction.class.getName());
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
    int size = 1;
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    DoubleVector o = db.get(db.getIDs().iterator().next());
    Random random = new Random(5);
    for(int i = 0; i < size; i++) {
      DoubleVector obj = o.randomInstance(random);
      insertions.add(new Pair<DoubleVector, DatabaseObjectMetadata>(obj, new DatabaseObjectMetadata()));
    }
    System.out.println("Insert " + insertions);
    System.out.println();
    db.insert(insertions);

    // delete objects
    for(Pair<DoubleVector, DatabaseObjectMetadata> insertion : insertions) {
      System.out.println("Delete id " + insertion.first.getID());
      db.delete(insertion.first.getID());
    }

    return result;
  }

  private static Database<DoubleVector> getDatabase() {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    // params.addParameter(AbstractDatabaseConnection.DATABASE_ID,
    // SpatialIndexDatabase.class);
    // params.addParameter(SpatialIndexDatabase.INDEX_ID, RdKNNTree.class);
    // params.addParameter(RdKNNTree.K_ID, k + 1);

    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    params.failOnErrors();
    if(params.hasUnusedParameters()) {
      // todo
      // fail("Unused parameters: " + params.getRemainingParameters());
    }

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);
    return db;
  }

}
