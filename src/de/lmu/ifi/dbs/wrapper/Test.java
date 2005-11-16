package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DBSCAN;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.*;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.FileBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.parser.DoubleDistanceParser;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.*;
import java.util.*;

/**
 * 
 */
public class Test {
  public static void main(String[] args) {
    // testCorrDist();
    testMTree();
  }

  private static void testMTree() {
    try {
      int k = 20;

//      File file1 = new File("LDM_1000.txt");
      File file1 = new File("Oldenburg_100a.txt");
      Parser parser1 = new DoubleDistanceParser();

//      File file1 = new File("2D_1K_uniform.txt");
//      Parser parser1 = new StandardLabelParser();

      InputStream in1 = new FileInputStream(file1);

      String[] param1 = {
      "-database", MkCoPTreeDatabase.class.getName()
      , "-" + MkNNTreeDatabase.K_P, "" + k
      , "-" + MkNNTreeDatabase.PAGE_SIZE_P, "4000"
      , "-" + MkNNTreeDatabase.CACHE_SIZE_P, "16000"
      , "-" + MkNNTreeDatabase.DISTANCE_FUNCTION_P, FileBasedDoubleDistanceFunction.class.getName()
      };

      parser1.setParameters(param1);
      Database<MetricalObject> db1 = parser1.parse(in1);
      System.out.println(db1);
      System.out.println("size db1 " + db1.size());

//      File file2 = new File("LDM_1000.txt");
      File file2 = new File("Oldenburg_100a.txt");
      Parser parser2 = new DoubleDistanceParser();

//      File file2 = new File("2D_1K_uniform.txt");
//      Parser parser2 = new StandardLabelParser();

      InputStream in2 = new FileInputStream(file2);

      String[] param2 = {"-database", SequentialDatabase.class.getName()};

      parser2.setParameters(param2);
      Database<MetricalObject> db2 = parser2.parse(in2);
      System.out.println("size db2 " + db2.size());

      DistanceFunction distFunction1 = new FileBasedDoubleDistanceFunction();
      distFunction1.setDatabase(db1, false);
//      DistanceFunction distFunction1 = new EuklideanDistanceFunction();

      DistanceFunction distFunction2 = new FileBasedDoubleDistanceFunction();
      distFunction2.setDatabase(db2, false);
//      DistanceFunction distFunction2 = new EuklideanDistanceFunction();

      Random random = new Random(210571);
      Iterator<Integer> it = db1.iterator();
      while (it.hasNext()) {
        int id = it.next();
        int kk = random.nextInt(k) + 1;
//        int kk = 5;
        System.out.println( id + " xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//        List<QueryResult> r1 = db1.kNNQueryForID(id, kk, distFunction1);
        List<QueryResult> r1 = db1.reverseKNNQuery(id, kk, distFunction1);
        System.out.println("r1 " + r1);

//        List<QueryResult> r2 = db2.kNNQueryForID(id, kk, distFunction2);
        List<QueryResult> r2 = db2.reverseKNNQuery(id, kk, distFunction2);
        System.out.println("r2 " + r2);

        System.out.println(db2.kNNQueryForID(7, kk, distFunction2));

        System.out.println("k " + kk);
//        System.out.println("r1.size() " + r1.size());
        System.out.println("r2.size() " + r2.size());
        System.out.println("r1 == r2 " + r1.equals(r2));
        if (! r1.equals(r2)) System.exit(1);
      }

//      System.out.println("I/O = " + ((IndexDatabase) db1).getIOAccess());

      // for (int i = 0; i < 450; i++) {
      // MetricalObject o = db1.get(new Integer(Integer.MIN_VALUE + i));
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
    try {
      Date start = new Date();
      // File file1 = new File("test.txt");
      // File file1 = new File("10_T_2.ascii");
      File file1 = new File("timeseries.txt");
      InputStream in1 = new FileInputStream(file1);
      Parser parser1 = new StandardLabelParser();
      String[] param1 = {"-database", "de.lmu.ifi.dbs.database.RTreeDatabase"
      // ,"-" + RTreeDatabase.FILE_NAME_P, "elki.idx"
      // ,"-" + RTreeDatabase.FLAT_DIRECTORY_F
      , "-" + SpatialIndexDatabase.BULK_LOAD_F, "-" + RTreeDatabase.CACHE_SIZE_P, "50000000", "-" + RTreeDatabase.PAGE_SIZE_P, "16000"};

      parser1.setParameters(param1);
      Database<FeatureVector> db1 = parser1.parse(in1);
      System.out.println(db1);

      long ms = new Date().getTime() - start.getTime();
      System.out.println("Total " + Util.format(ms / 1000.0) + " s");

      // File file2 = new File("test.txt");
      // File file2 = new File("10_T_2.ascii");
      File file2 = new File("timeseries.txt");
      InputStream in2 = new FileInputStream(file2);
      Parser parser2 = new StandardLabelParser();
      String[] param2 = {"-database", "de.lmu.ifi.dbs.database.RTreeDatabase"
      // ,"-" + RTreeDatabase.FILE_NAME_P, "elki.idx"
      // ,"-" + RTreeDatabase.FLAT_DIRECTORY_F
      // ,"-" + SpatialIndexDatabase.BULK_LOAD_F
      , "-" + RTreeDatabase.CACHE_SIZE_P, "50000000", "-" + RTreeDatabase.PAGE_SIZE_P, "16000"};

      start = new Date();
      parser2.setParameters(param2);
      Database<FeatureVector> db2 = parser2.parse(in2);
      System.out.println(db2);
      ms = new Date().getTime() - start.getTime();
      System.out.println("Total " + Util.format(ms / 1000.0) + " s");

      EuklideanDistanceFunction distFunction = new EuklideanDistanceFunction();
      List<QueryResult> r1 = db1.kNNQueryForID(300, 10, distFunction);
      System.out.println("r1 " + r1);

      List<QueryResult> r2 = db2.kNNQueryForID(300, 10, distFunction);
      System.out.println("r2 " + r2);

      // for (int i = 0; i < 450; i++) {
      // MetricalObject o = db1.get(new Integer(Integer.MIN_VALUE + i));
      // db1.delete(o);
      // System.out.println(db1);
      // }
    }
    catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use
      // File | Settings | File Templates.
    }

  }

  private static void testCorrDist() throws FileNotFoundException, UnableToComplyException {
    File file = new File("test.txt");
    // File file = new File("10_T_2.ascii");
    InputStream in = new FileInputStream(file);
    Parser parser = new StandardLabelParser();
    String[] param = {"-database", "de.lmu.ifi.dbs.database.SequentialDatabase", "-epsilon", "2x1000", "-minpts", "10", "-distancefunction", "de.lmu.ifi.dbs.distance.CorrelationDistanceFunction", "-verbose"};
    param = parser.setParameters(param);
    Database db = parser.parse(in);

    System.out.println(Arrays.asList(param));
    Algorithm dbscan = new DBSCAN();
    dbscan.setParameters(param);
    dbscan.run(db);
    Result result = dbscan.getResult();
    result.output(null, null, null);
  }

}
