package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNJoinResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.DirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Joins in a given spatial database to each object its k-nearest neighbors.
 * This algorithm only supports spatial databases based on a spatial index
 * structure.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KNNJoin<O extends RealVector>
extends DistanceBasedAlgorithm<O> {

  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level level = Level.OFF;

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>k";

  /**
   * Parameter k.
   */
  private int k;

  /**
   * The knn lists for each object.
   */
  private KNNJoinResult<O> result;

  /**
   * Creates a new KNNJoin algorithm. Sets parameter k to the optionhandler
   * additionally to the parameters provided by super-classes. Since KNNJoin
   * is a non-abstract class, finally optionHandler is initialized.
   */
  public KNNJoin() {
    super();
    initLogger();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Runs the algorithm.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  @SuppressWarnings({"ForLoopReplaceableByForEach"})
  public void runInTime(Database<O> database) throws IllegalStateException {
    if (!(database instanceof SpatialIndexDatabase))
      throw new IllegalArgumentException("Database must be an instance of " + SpatialIndexDatabase.class.getName());

    if (!(getDistanceFunction() instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance Function must be an instance of " + SpatialDistanceFunction.class.getName());

    SpatialIndexDatabase<O> db = (SpatialIndexDatabase<O>) database;
    //noinspection unchecked
    SpatialDistanceFunction<O, Distance> distFunction = (SpatialDistanceFunction<O, Distance>) getDistanceFunction();
    distFunction.setDatabase(db, false);

    HashMap<Integer, KNNList<Distance>> knnLists = new HashMap<Integer, KNNList<Distance>>();

    try {
      // data pages of s
      List<DirectoryEntry> ps_candidates = db.getLeaves();
      Progress progress = new Progress(db.size());
      logger.info("# ps = " + ps_candidates.size());

      // data pages of r
      List<DirectoryEntry> pr_candidates = new ArrayList<DirectoryEntry>(ps_candidates);
      logger.info("# pr = " + pr_candidates.size());

      int processed = 0;
      int processedPages = 0;
      boolean up = true;
      for (int r = 0; r < pr_candidates.size(); r++) {
        DirectoryEntry pr_entry = pr_candidates.get(r);
        MBR pr_mbr = pr_entry.getMBR();
        SpatialNode pr = db.getNode(pr_entry.getID());
        Distance pr_knn_distance = distFunction.infiniteDistance();
        logger.info(" ------ PR = " + pr);

        // create for each data object a knn list
        for (int j = 0; j < pr.getNumEntries(); j++) {
          knnLists.put(pr.getEntry(j).getID(), new KNNList<Distance>(k, getDistanceFunction().infiniteDistance()));
        }

        if (up) {
          for (int s = 0; s < ps_candidates.size(); s++) {
            DirectoryEntry ps_entry = ps_candidates.get(s);
            MBR ps_mbr = ps_entry.getMBR();
            Distance distance = distFunction.distance(pr_mbr, ps_mbr);

            if (distance.compareTo(pr_knn_distance) <= 0) {
              SpatialNode ps = db.getNode(ps_entry.getID());
              pr_knn_distance = processDataPages(pr, ps, knnLists, pr_knn_distance);
            }
          }
          up = false;
        }

        else {
          for (int s = ps_candidates.size() - 1; s >= 0; s--) {
            DirectoryEntry ps_entry = ps_candidates.get(s);
            MBR ps_mbr = ps_entry.getMBR();
            Distance distance = distFunction.distance(pr_mbr, ps_mbr);

            if (distance.compareTo(pr_knn_distance) <= 0) {
              SpatialNode ps = db.getNode(ps_entry.getID());
              pr_knn_distance = processDataPages(pr, ps, knnLists, pr_knn_distance);
            }
          }
          up = true;
        }

        processed += pr.getNumEntries();

        if (isVerbose()) {
          progress.setProcessed(processed);
          System.out.print("\r" + progress.toString() + " Number of processed data pages: " + processedPages++);
        }
      }
      result = new KNNJoinResult<O>(knnLists);
      System.out.println("\nKNN-Join I/O = " + db.getIOAccess());

//      Iterator<Integer> it = db.iterator();
//      while (it.hasNext()) {
//        Integer id = it.next();
//        Distance dbDist = db.kNNQuery(id, k, distFunction).get(k - 1).getDistance();
//        Distance joinDist = result.getKNNDistance(id);
//
//        if (! dbDist.equals(joinDist)) {
//          System.out.println("id " + id);
//          System.out.println("dbDist    " + dbDist);
//          System.out.println("joinDist  " + joinDist);
//          System.out.println("db knns   " + db.kNNQuery(id, k, distFunction));
//          System.out.println("join " + result.getKNNs(id));
//          System.exit(1);
//        }
//      }
//      System.out.println("knn join ok");
    }

    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * Processes the two data pages pr and ps and determines the k-neraest
   * neighors of pr in ps.
   *
   * @param pr              the first data page
   * @param ps              the second data page
   * @param knnLists        the knn lists for each data object
   * @param pr_knn_distance the current knn distance of data page pr
   */
  private Distance processDataPages(SpatialNode pr, SpatialNode ps,
                                    HashMap<Integer, KNNList<Distance>> knnLists,
                                    Distance pr_knn_distance) {

    //noinspection unchecked
    boolean infinite = getDistanceFunction().isInfiniteDistance(pr_knn_distance);
    for (int i = 0; i < pr.getNumEntries(); i++) {
      Integer r_id = pr.getEntry(i).getID();
      KNNList<Distance> knnList = knnLists.get(r_id);

      for (int j = 0; j < ps.getNumEntries(); j++) {
        Integer s_id = ps.getEntry(j).getID();

        Distance distance = getDistanceFunction().distance(r_id, s_id);
        if (knnList.add(new QueryResult<Distance>(s_id, distance))) {
          // set kNN distance of r
          if (infinite)
            pr_knn_distance = knnList.getMaximumDistance();
          pr_knn_distance = Util.max(knnList.getMaximumDistance(), pr_knn_distance);
        }
      }
    }
    return pr_knn_distance;
  }

  /**
   * Sets the parameters k to the parameters set by the super-class' method.
   * Parameter k is required.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      getDistanceFunction().valueOf(optionHandler.getOptionValue(K_P));
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(K_P, Integer.toString(k));

    return result;
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("KNN-Join", "K-Nearest Neighbor Join", "Algorithm to find the k-nearest neighbors of each object in a spatial database.", "");
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }
}
