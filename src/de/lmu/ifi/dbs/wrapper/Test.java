package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.*;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 
 */
public class Test {
  public static void main(String[] args) {
    testMTree();
  }

  private static void testMTree() {
    try {
      int k = 50;

//      String fileName = "LDM_1000.txt";
//      String fileName = "Oldenburg_100a.txt";
//      String fileName = "5_T_2.txt";
      String fileName = "2D_1K_uniform.txt";

      FileBasedDatabaseConnection<DatabaseObject> con1 = new FileBasedDatabaseConnection<DatabaseObject>();

      String[] param1 = {
      "-" + AbstractDatabaseConnection.DATABASE_CLASS_P, MkCoPTreeDatabase.class.getName()
      , "-" + InputStreamDatabaseConnection.PARSER_P, DoubleVectorLabelParser.class.getName()
      , "-" + FileBasedDatabaseConnection.INPUT_P, fileName
      , "-" + MkMaxTreeDatabase.K_P, "" + k
      , "-" + MkMaxTreeDatabase.PAGE_SIZE_P, "4000"
      , "-" + MkMaxTreeDatabase.CACHE_SIZE_P, "16000"
//      , "-" + MkNNTreeDatabase.DISTANCE_FUNCTION_P, FileBasedDoubleDistanceFunction.class.getName()
      };

      con1.setParameters(param1);
      MkCoPTreeDatabase<DatabaseObject, DoubleDistance> db1 =
      (MkCoPTreeDatabase<DatabaseObject, DoubleDistance>) con1.getDatabase(null);
      System.out.println(db1);
      System.out.println("size db1 " + db1.size());

      ///////////
      FileBasedDatabaseConnection<DatabaseObject> con2 = new FileBasedDatabaseConnection<DatabaseObject>();

      String[] param2 = {
      "-" + AbstractDatabaseConnection.DATABASE_CLASS_P, SequentialDatabase.class.getName()
      , "-" + InputStreamDatabaseConnection.PARSER_P, DoubleVectorLabelParser.class.getName()
      , "-" + FileBasedDatabaseConnection.INPUT_P, fileName
      };

      con2.setParameters(param2);
      Database<DatabaseObject> db2 = con2.getDatabase(null);
      System.out.println("size db2 " + db2.size());

//      DistanceFunction distFunction1 = new FileBasedDoubleDistanceFunction();
      DistanceFunction distFunction1 = new EuklideanDistanceFunction();
      distFunction1.setDatabase(db1, false);

//      DistanceFunction distFunction2 = new FileBasedDoubleDistanceFunction();
      DistanceFunction distFunction2 = new EuklideanDistanceFunction();
      distFunction2.setDatabase(db2, false);

      Random random = new Random(210571);
      Iterator<Integer> it = db1.iterator();
      int i = 0;
      while (it.hasNext()) {
        i++;
        if (i == 20) break;
        int id = it.next();
        int kk = random.nextInt(k) + 1;

//        for (int kkk = 1; kkk <= k; kkk++) {
//          List<QueryResult> qr = db1.kNNQueryForID(id, kkk, distFunction1);
//          List<QueryResult> qr = db1.reverseKNNQuery(id, kkk, distFunction1);
//          Distance kDist = qr.get(qr.size()-1).getDistance();
//          System.out.println("kDIst[" + kkk + "] = " + kDist);
//          System.out.println("kDIst[" + kkk + "] = " + qr.get(qr.size()-1) +  " " +kDist);
//        }

//        int kk = 5;
        System.out.println(id + " xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//        List<QueryResult> r1 = db1.kNNQueryForID(id, kk, distFunction1);
        List<QueryResult> r1 = db1.reverseKNNQuery(id, kk, distFunction1);
        System.out.println("r1 " + r1);

//        List<QueryResult> r2 = db2.kNNQueryForID(id, kk, distFunction2);
        List<QueryResult> r2 = db2.reverseKNNQuery(id, kk, distFunction2);
        System.out.println("r2 " + r2);

        System.out.println("k " + kk);
//        System.out.println("r1.size() " + r1.size());
        System.out.println("r2.size() " + r2.size());
        System.out.println("r1 == r2 " + r1.equals(r2));
//        if (! r1.equals(r2)) System.exit(1);
      }

      System.out.println(db1.getRkNNStatistics());

//      System.out.println("I/O = " + ((IndexDatabase) db1).getIOAccess());

      // for (int i = 0; i < 450; i++) {
      // DatabaseObject o = db1.get(new Integer(Integer.MIN_VALUE + i));
      // db1.delete(o);
      // System.out.println(db1);
      // }
    }
    catch (Exception e) {
      e.printStackTrace(); // To change body of catch statement use
      // File | Settings | File Templates.
    }

  }

  private static void testRTree() throws FileNotFoundException {
    Date start = new Date();

    // String fileName = "test.txt";
    // String fileName = "10_T_2.ascii";
    String fileName = "timeseries.txt";

    FileBasedDatabaseConnection<FeatureVector> con1 = new FileBasedDatabaseConnection<FeatureVector>();
    String[] param1 = {
    "-" + AbstractDatabaseConnection.DATABASE_CLASS_P, RTreeDatabase.class.getName()
    , "-" + InputStreamDatabaseConnection.PARSER_P, DoubleVectorLabelParser.class.getName()
    , "-" + FileBasedDatabaseConnection.INPUT_P, fileName
    , "-" + SpatialIndexDatabase.BULK_LOAD_F
    , "-" + RTreeDatabase.CACHE_SIZE_P, "50000000"
    , "-" + RTreeDatabase.PAGE_SIZE_P, "16000"
    };


    con1.setParameters(param1);
    Database<FeatureVector> db1 = con1.getDatabase(null);
    System.out.println(db1);
    long ms = new Date().getTime() - start.getTime();
    System.out.println("Total " + Util.format(ms / 1000.0) + " s");

    ///////////////////////////////////////////////
    FileBasedDatabaseConnection<FeatureVector> con2 = new FileBasedDatabaseConnection<FeatureVector>();
    String[] param2 = {
    "-" + AbstractDatabaseConnection.DATABASE_CLASS_P, RTreeDatabase.class.getName()
    , "-" + InputStreamDatabaseConnection.PARSER_P, DoubleVectorLabelParser.class.getName()
    , "-" + FileBasedDatabaseConnection.INPUT_P, fileName
    // ,"-" + RTreeDatabase.FILE_NAME_P, "elki.idx"
    // ,"-" + RTreeDatabase.FLAT_DIRECTORY_F
    , "-" + RTreeDatabase.CACHE_SIZE_P, "50000000"
    , "-" + RTreeDatabase.PAGE_SIZE_P, "16000"
    };

    start = new Date();
    con2.setParameters(param2);
    Database<FeatureVector> db2 = con2.getDatabase(null);
    System.out.println(db2);
    ms = new Date().getTime() - start.getTime();
    System.out.println("Total " + Util.format(ms / 1000.0) + " s");

    EuklideanDistanceFunction distFunction = new EuklideanDistanceFunction();
    List<QueryResult> r1 = db1.kNNQueryForID(300, 10, distFunction);
    System.out.println("r1 " + r1);

    List<QueryResult> r2 = db2.kNNQueryForID(300, 10, distFunction);
    System.out.println("r2 " + r2);

    // for (int i = 0; i < 450; i++) {
    // DatabaseObject o = db1.get(new Integer(Integer.MIN_VALUE + i));
    // db1.delete(o);
    // System.out.println(db1);
    // }
  }
}
