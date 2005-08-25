package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Date;

/**
 * Wrapper class for COPAC algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPACWrapper {

  public static void main(String[] args) {

    try {
      Date start = new Date();
      //      File file1 = new File("test.txt");
//      File file1 = new File("10_T_2.ascii");
      File file1 = new File("timeseries.txt");
      InputStream in1 = new FileInputStream(file1);
      Parser parser1 = new StandardLabelParser();

      String[] param1 = {
      "-algorithm", "COPAC"
      , "-partAlg", "DBSCAN"
      , "-preprocessor", "de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor"
//      , "-epsilon", "3"
      , "-epsilon", "1x100000"
      , "-minpts", "7"
      , "-database", "de.lmu.ifi.dbs.database.SequentialDatabase"
//      , "-distancefunction", "de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction"
      , "-distancefunction", CorrelationDistanceFunction.class.getName()
      , "-in", "data/synthetic/test.txt"
//      , "-out", "results/partitionTest/partitionT1"
      , "-out", "results/partitionTest/partitionT2"
      , "-verbose",
      //      ,"-" + RTreeDatabase.FILE_NAME_P, "elki.idx"
      //      ,"-" + RTreeDatabase.FLAT_DIRECTORY_F
      //    , "-" + SpatialIndexDatabase.BULK_LOAD_F
      //    , "-" + RTreeDatabase.CACHE_SIZE_P, "50000000"
      //    , "-" + RTreeDatabase.PAGE_SIZE_P, "16000"
      };


      KDDTask task = new KDDTask();
      task.setParameters(param1);
      task.run();

//      parser1.setParameters(param1);
//      Database<RealVector> db1 = parser1.parse(in1);
//      System.out.println(db1);

//      Algorithm copac = new COPAC();

    }
    catch (FileNotFoundException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }


  }


}
