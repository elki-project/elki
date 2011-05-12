package experimentalcode.marisa.tests;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.marisa.index.xtree.common.XTree;
import experimentalcode.marisa.index.xtree.util.SquareEuclideanDistanceFunction;
import experimentalcode.marisa.utils.PriorityQueue;
import experimentalcode.marisa.utils.Zeit;

public class KNNTests {

  public static <O extends NumberVector<O, ?>> List<DistanceResultPair<DoubleDistance>> sequKNN(int k, List<O> objects, O query, SpatialPrimitiveDistanceFunction<O, DoubleDistance> df) {
    PriorityQueue<O> pq = new PriorityQueue<O>(false, k);
    for(Iterator<O> iterator = objects.iterator(); iterator.hasNext();) {
      O o = iterator.next();
      double dist = df.distance(o, query).getValue();
      pq.addSecure(dist, o, k);
    }
    List<DistanceResultPair<DoubleDistance>> result = new ArrayList<DistanceResultPair<DoubleDistance>>();
    while(!pq.isEmpty()) {
      result.add(0, new DistanceResultPair<DoubleDistance>(new DoubleDistance(pq.firstPriority()), pq.removeFirst().getDBID()));
    }
    return result;
  }

  public static <O extends NumberVector<O, ?>> void knnCorrectTest(int k, List<O> objects, AbstractRStarTree<O, ?, ?> index, List<O> queries) {
    SquareEuclideanDistanceFunction ed = new SquareEuclideanDistanceFunction();
    int queryNumber = 0;
    for(; queryNumber < queries.size(); queryNumber++) {
      O nv = queries.get(queryNumber);
      List<DistanceResultPair<DoubleDistance>> result = index.kNNQuery(nv, k, ed);
      List<DistanceResultPair<DoubleDistance>> resultS = sequKNN(k, objects, nv, ed);
      assert result.size() == resultS.size() : "index: " + result.size() + "; sequ: " + resultS.size() + "; k: " + k;
      if(!result.get(result.size() - 1).getDistance().getValue().equals(resultS.get(result.size() - 1).getDistance().getValue()) || !result.get(0).getDistance().getValue().equals(resultS.get(0).getDistance().getValue())) {
        throw new RuntimeException("whoops: " + result.get(result.size() - 1).getDistance().getValue() + " != " + resultS.get(result.size() - 1).getDistance().getValue() + " || " + result.get(0).getDistance().getValue() + " != " + resultS.get(0).getDistance().getValue() + "\nindex: " + result.toString() + "\nsequ:  " + resultS.toString());
      }
    }
  }

  public static <O extends NumberVector<O, ?>> void knnRun(int k, AbstractRStarTree<O, ?, ?> index, List<O> queries) {
    SquareEuclideanDistanceFunction sed = new SquareEuclideanDistanceFunction();
    int queryNumber = 0;
    for(; queryNumber < queries.size(); queryNumber++) {
      O nv = queries.get(queryNumber);
      // System.out.println(queryNumber);
      List<DistanceResultPair<DoubleDistance>> result = index.kNNQuery(nv, k, sed);
      assert result.size() == k;
    }
  }

  public static void speedTest(String xtFileName, String queryFileName) throws ParameterException, NumberFormatException, IOException {
    XTree<DoubleVector> xt;
    xt = XTreeTests.loadXTree(xtFileName);
    System.out.println("XT: " + xt.toString());
    speedTest(xt, queryFileName);
  }

  public static void speedTest(XTree<DoubleVector> xt, String queryFileName) throws NumberFormatException, IOException {
    List<DoubleVector> queries = new ArrayList<DoubleVector>();

    FileInputStream fis = new FileInputStream(queryFileName);
    DataInputStream in = new DataInputStream(fis);
    int stop = 10000;
    for(int i = 0; (in.available() != 0) && i < stop; i++) {
      queries.add(DBIDUtil.importInteger(i), XTreeTests.readNext(in, xt.getDimensionality()));
    }
    in.close();
    fis.close();
    System.out.println("loaded QUERY DB of size " + queries.size());

    int k = 10;

    Date now = new Date();
    knnRun(k, xt, queries);
    System.out.println("Took " + Zeit.wieLange(now));
    for(int i = 0; i < 14; i++) {
      now = new Date();
      knnRun(k, xt, queries);
      System.out.println("Took " + Zeit.wieLange(now));
    }
    System.out.println("Done with the warming up");
    xt.resetPageAccess();
    now = new Date();
    knnRun(k, xt, queries);
    System.out.println("Took " + Zeit.wieLange(now)+ ", logical: "+xt.getLogicalPageAccess()+", physical read: "+xt.getPhysicalReadAccess());
  }

  public static void amICorrect() throws ParameterException, NumberFormatException, IOException {
    XTree<DoubleVector> xt;
    xt = XTreeTests.loadXTree();
    System.out.println("XT: " + xt.toString());
    List<DoubleVector> db = new ArrayList<DoubleVector>(), queries = new ArrayList<DoubleVector>();

    FileInputStream fis = new FileInputStream("C:/WORK/Theseus/data/synthetic/15DUniform.csv");
    DataInputStream in = new DataInputStream(fis);
    int stop = 10000;
    for(int i = 0; (in.available() != 0); i++) {
      if(i == stop)
        break;
      db.add(XTreeTests.readNext(in, i));
    }
    in.close();
    fis.close();
    System.out.println("loaded DB of size " + db.size());
    fis = new FileInputStream("C:/WORK/Theseus/data/synthetic/15Dqueries.csv");
    in = new DataInputStream(fis);
    stop = 100;
    for(int i = 0; (in.available() != 0); i++) {
      if(i == stop)
        break;
      queries.add(XTreeTests.readNext(in, i));
    }
    in.close();
    fis.close();
    System.out.println("loaded QUERY DB of size " + queries.size());

    knnCorrectTest(10, db, xt, queries);
  }

  public static void amICorrect(XTree<DoubleVector> xt, String queryFileName) throws NumberFormatException, IOException {
    List<DoubleVector> db = new ArrayList<DoubleVector>(), queries = new ArrayList<DoubleVector>();
    FileInputStream fis = new FileInputStream("C:/WORK/Theseus/data/synthetic/15DUniform.csv");
    DataInputStream in = new DataInputStream(fis);
    long stop = xt.getSize();
    for(long i = 0; (in.available() != 0); i++) {
      if(i == stop)
        break;
      db.add(XTreeTests.readNext(in, (int) i, xt.getDimensionality()));
    }
    in.close();
    fis.close();
    System.out.println("loaded DB of size " + db.size());
    fis = new FileInputStream(queryFileName);
    in = new DataInputStream(fis);
    stop = 100;
    for(int i = 0; (in.available() != 0); i++) {
      if(i == stop)
        break;
      queries.add(XTreeTests.readNext(in, i, xt.getDimensionality()));
    }
    in.close();
    fis.close();
    System.out.println("loaded QUERY DB of size " + queries.size());

    knnCorrectTest(10, db, xt, queries);
  }

  public static void main(String[] args) throws ParameterException, NumberFormatException, IOException {
    // amICorrect();
    speedTest("C:/WORK/Theseus/Experimente/xtrees/15DUniformXTree_default_mO1", "C:/WORK/Theseus/data/synthetic/15Dqueries.csv");
  }
}
