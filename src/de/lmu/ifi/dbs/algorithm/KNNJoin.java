package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNJoinResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

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
public class KNNJoin<O extends NumberVector, D extends Distance<D>, N extends SpatialNode<E>, E extends SpatialEntry> extends DistanceBasedAlgorithm<O, D> {

  /**
   * Holds the class specific debug status.
   */
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>specifies the kNN to be assigned (>1)";

  /**
   * Parameter k.
   */
  private int k;

  /**
   * The knn lists for each object.
   */
  private KNNJoinResult<O, D> result;

  /**
   * Creates a new KNNJoin algorithm. Sets parameter k to the optionhandler
   * additionally to the parameters provided by super-classes. Since KNNJoin
   * is a non-abstract class, finally optionHandler is initialized.
   */
  public KNNJoin() {
    super();
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
  protected
  @SuppressWarnings({"ForLoopReplaceableByForEach"})
  void runInTime(Database<O> database) throws IllegalStateException {
    if (!(database instanceof SpatialIndexDatabase)) {
      throw new IllegalArgumentException("Database must be an instance of " + SpatialIndexDatabase.class.getName());
    }
    if (!(getDistanceFunction() instanceof SpatialDistanceFunction)) {
      throw new IllegalArgumentException("Distance Function must be an instance of " + SpatialDistanceFunction.class.getName());
    }
    SpatialIndexDatabase<O,N,E> db = (SpatialIndexDatabase<O,N,E>) database;
    SpatialDistanceFunction<O, D> distFunction = (SpatialDistanceFunction<O, D>) getDistanceFunction();
    distFunction.setDatabase(db, isVerbose(), isTime());

    HashMap<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    try {
      // data pages of s
      List<E> ps_candidates = db.getLeaves();
      Progress progress = new Progress(this.getClass().getName(), db.size());
      if (DEBUG) {
        logger.fine("# ps = " + ps_candidates.size());
      }
      // data pages of r
      List<E> pr_candidates = new ArrayList<E>(ps_candidates);
      if (DEBUG) {
        logger.fine("# pr = " + pr_candidates.size());
      }
      int processed = 0;
      int processedPages = 0;
      boolean up = true;
      for (int r = 0; r < pr_candidates.size(); r++) {
        E pr_entry = pr_candidates.get(r);
        MBR pr_mbr = pr_entry.getMBR();
        N pr = db.getIndex().getNode(pr_entry);
        D pr_knn_distance = distFunction.infiniteDistance();
        if (DEBUG) {
          logger.fine(" ------ PR = " + pr);
        }
        // create for each data object a knn list
        for (int j = 0; j < pr.getNumEntries(); j++) {
          knnLists.put(pr.getEntry(j).getID(), new KNNList<D>(k, getDistanceFunction().infiniteDistance()));
        }

        if (up) {
          for (int s = 0; s < ps_candidates.size(); s++) {
            E ps_entry = ps_candidates.get(s);
            MBR ps_mbr = ps_entry.getMBR();
            D distance = distFunction.distance(pr_mbr, ps_mbr);

            if (distance.compareTo(pr_knn_distance) <= 0) {
              SpatialNode ps = db.getIndex().getNode(ps_entry);
              pr_knn_distance = processDataPages(pr, ps, knnLists, pr_knn_distance);
            }
          }
          up = false;
        }

        else {
          for (int s = ps_candidates.size() - 1; s >= 0; s--) {
            E ps_entry = ps_candidates.get(s);
            MBR ps_mbr = ps_entry.getMBR();
            D distance = distFunction.distance(pr_mbr, ps_mbr);

            if (distance.compareTo(pr_knn_distance) <= 0) {
              SpatialNode ps = db.getIndex().getNode(ps_entry);
              pr_knn_distance = processDataPages(pr, ps, knnLists, pr_knn_distance);
            }
          }
          up = true;
        }

        processed += pr.getNumEntries();

        if (isVerbose()) {
          progress.setProcessed(processed);
          logger.log(new ProgressLogRecord(Level.INFO, "\r" + progress.toString() + " Number of processed data pages: " + processedPages++, progress.getTask(), progress.status()));
        }
      }
      result = new KNNJoinResult<O, D>(knnLists);
      
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
  private D processDataPages(SpatialNode pr, SpatialNode ps, HashMap<Integer, KNNList<D>> knnLists, D pr_knn_distance) {

    //noinspection unchecked
    boolean infinite = getDistanceFunction().isInfiniteDistance(pr_knn_distance);
    for (int i = 0; i < pr.getNumEntries(); i++) {
      Integer r_id = pr.getEntry(i).getID();
      KNNList<D> knnList = knnLists.get(r_id);

      for (int j = 0; j < ps.getNumEntries(); j++) {
        Integer s_id = ps.getEntry(j).getID();

        D distance = getDistanceFunction().distance(r_id, s_id);
        if (knnList.add(new QueryResult<D>(s_id, distance))) {
          // set kNN distance of r
          if (infinite) {
            pr_knn_distance = knnList.getMaximumDistance();
          }
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String kString = optionHandler.getOptionValue(K_P);
    try {
      k = Integer.parseInt(kString);
      if (k <= 1) {
        throw new WrongParameterValueException(K_P, kString, K_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(K_P, kString, K_D, e);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k));

    return attributeSettings;
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

}
