package experimentalcode.elke.algorithm.lof;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
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
    ListParameterization params1 = new ListParameterization();
    params1.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    params1.addParameter(LOF.K_ID, k);
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params1);

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);
    // db = getDatabase();

    // /XXX
    DBIDs ids_sample = db.randomSample(6, 0);
    ids_sample = db.getIDs();
    for(Iterator<DBID> it = db.iterator(); it.hasNext();) {
      DBID id = it.next();
      System.out.println(id);
    }
    System.out.println("XXXsample " + ids_sample);
    System.exit(0);

    Integer[] insertion_ids = new Integer[] { 1, 16, 7 };
    Integer[] insertion_ids2 = new Integer[] { 97, 67, 56 };
    // 16,7,67,56};
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    for(Integer iid : insertion_ids) {
      DBID id = DBIDUtil.importInteger(iid);
      DoubleVector o = db.delete(id);
      insertions.add(new Pair<DoubleVector, DatabaseObjectMetadata>(o, new DatabaseObjectMetadata(db, id)));
    }
    List<Pair<DoubleVector, DatabaseObjectMetadata>> insertions2 = new ArrayList<Pair<DoubleVector, DatabaseObjectMetadata>>();
    for(Integer iid : insertion_ids2) {
      DBID id = DBIDUtil.importInteger(iid);
      DoubleVector o = db.delete(id);
      insertions2.add(new Pair<DoubleVector, DatabaseObjectMetadata>(o, new DatabaseObjectMetadata(db, id)));
    }

    // setup algorithm
    OnlineLOF<DoubleVector, DoubleDistance> lof = null;
    Class<OnlineLOF<DoubleVector, DoubleDistance>> lofcls = ClassGenericsUtil.uglyCastIntoSubclass(OnlineLOF.class);
    lof = params1.tryInstantiate(lofcls, OnlineLOF.class);
    params1.failOnErrors();
    if(params1.hasUnusedParameters()) {
      // fail("Unused parameters: " + params1.getRemainingParameters());
    }

    // run LOF on database
    // lof.setVerbose(true);
    OutlierResult result1 = lof.run(db);

    System.out.println(insertions.get(0).first);
    System.out.println(insertions.get(0).second);
    
    db.insert(insertions.get(0));
    db.insert(insertions2);

    OutlierResult result2 = runLOF();

    DBIDs ids = db.getIDs();
    AnnotationResult<Double> scores1 = result1.getScores();
    AnnotationResult<Double> scores2 = result2.getScores();
    for(DBID id : ids) {
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

  private static OutlierResult runLOF() {
    Database<DoubleVector> db = getDatabase();
    System.out.println("hallo");
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, k);

    // setup algorithm
    LOF<DoubleVector, DoubleDistance> lof = null;
    Class<LOF<DoubleVector, DoubleDistance>> lofcls = ClassGenericsUtil.uglyCastIntoSubclass(LOF.class);
    lof = params.tryInstantiate(lofcls, lofcls);
    params.failOnErrors();

    // run LOF on database
    return lof.run(db);
  }

  private static Database<DoubleVector> getDatabase() {
    ListParameterization params = new ListParameterization();
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);
    //params.addParameter(AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class);
    //params.addParameter(SpatialIndexDatabase.INDEX_ID, RdKNNTree.class);
    //params.addParameter(RdKNNTree.K_ID, k + 1);

    System.out.println("hallo");
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    params.failOnErrors();
    System.out.println("hallo");

    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);
    return db;
  }

}
