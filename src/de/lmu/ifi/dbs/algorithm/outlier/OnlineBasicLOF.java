package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.LOFResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts.
 *
 * @author Peer Kr&ouml;ger (<a
 *         href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */
public class OnlineBasicLOF<O extends DatabaseObject> extends
    DistanceBasedAlgorithm<O, DoubleDistance> {

  /**
   * The default pagesize.
   */
  public static final int DEFAULT_PAGE_SIZE = 4000;

  /**
   * Parameter pagesize.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "a positive integer value specifying the size of a page in bytes, "
                                           + "default is " + DEFAULT_PAGE_SIZE + " Byte.";

  /**
   * The default cachesize.
   */
  public static final int DEFAULT_CACHE_SIZE = Integer.MAX_VALUE;

  /**
   * Parameter cachesize.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_SIZE_D = "a positive integer value specifying the size of the cache in bytes, "
                                            + "default is Integer.MAX_VALUE Byte.";

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "positive number of nearest neighbors of an object to be considered for computing its LOF.";

  /**
   * Minimum points.
   */
  int minpts;

  /**
   * The size of a page in Bytes.
   */
  int pageSize;

  /**
   * The size of the cache in Bytes.
   */
  int cacheSize;

  /**
   * Provides the result of the algorithm.
   */
  LOFResult<O> result;

  /**
   * The table for nearest and reverse nearest neighbors.
   */
  NNTable nnTable;

  /**
   * The table for neares and reverse nearest neighbors.
   */
  LOFTable lofTable;

  /**
   * Sets minimum points to the optionhandler additionally to the parameters
   * provided by super-classes.
   */
  public OnlineBasicLOF() {
    super();
    // parameter page size
    IntParameter pageSize = new IntParameter(PAGE_SIZE_P, PAGE_SIZE_D, new GreaterConstraint(0));
    pageSize.setDefaultValue(DEFAULT_PAGE_SIZE);
    optionHandler.put(PAGE_SIZE_P, pageSize);

    // parameter cache size
    IntParameter cacheSize = new IntParameter(CACHE_SIZE_P, CACHE_SIZE_D, new GreaterConstraint(0));
    cacheSize.setDefaultValue(DEFAULT_CACHE_SIZE);
    optionHandler.put(CACHE_SIZE_P, cacheSize);

    //parameter minpts
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    if (isVerbose()) {
      verbose("\n##### Computing LOFs:");
    }

    {// compute neighbors of each db object
      if (isVerbose()) {
        verbose("\nStep 1: computing neighborhoods:");
      }
      Progress progressNeighborhoods = new Progress("LOF", database.size());
      int counter = 1;
      nnTable = new NNTable(pageSize, cacheSize, minpts);
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        computeNeighbors(database, id);
        if (isVerbose()) {
          progressNeighborhoods.setProcessed(counter);
          progress(progressNeighborhoods);
        }
      }
      if (isVerbose()) {
        verbose("");
      }
    }

    {// computing reachability distances
      if (isVerbose()) {
        verbose("\nStep 2: computing reachability distances:");
      }
      Progress progressNeighborhoods = new Progress("LOF", database.size());
      int counter = 1;
      for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
        Integer id = iter.next();
        nnTable.computeReachabilityDistances(id);
        if (isVerbose()) {
          progressNeighborhoods.setProcessed(counter);
          progress(progressNeighborhoods);
        }
      }
      if (isVerbose()) {
        verbose("");
      }
    }

    {// compute LOF of each db object
      if (isVerbose()) {
        verbose("\n Step 3: computing LOFs:");
      }
      // keeps the lofs for each object
      lofTable = new LOFTable(pageSize, cacheSize, minpts);
      {
        Progress progressLOFs = new Progress("LOF: LOF for objects",
                                             database.size());
        int counter = 0;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
          Integer id = iter.next();
          computeLOF(id);
          if (isVerbose()) {
            progressLOFs.setProcessed(counter + 1);
            progress(progressLOFs);
          }
        }
        if (isVerbose()) {
          verbose("");
        }
      }
      result = new LOFResult<O>(database, lofTable, nnTable);

      if (isTime()) {
        verbose("\nPhysical read Access LOF-Table: "
                + lofTable.getPhysicalReadAccess());

        verbose("Physical write Access LOF-Table: "
                + lofTable.getPhysicalWriteAccess());

        verbose("Logical page Access LOF-Table:  "
                + lofTable.getLogicalPageAccess());

        verbose("Physical read Access NN-Table:  "
                + nnTable.getPhysicalReadAccess());

        verbose("Physical write Access NN-Table:  "
                + nnTable.getPhysicalWriteAccess());

        verbose("Logical page Access NN-Table:   "
                + nnTable.getLogicalPageAccess());
      }
    }
  }

  /**
   * Computes the minpts-nearest neighbors of a given object in a given
   * database and inserts them into the nnTable.
   *
   * @param database the database containing the objects
   * @param id       the object id
   */
  public void computeNeighbors(Database<O> database, Integer id) {
    List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(
        id, minpts + 1, getDistanceFunction());
    neighbors.remove(0);

    for (int k = 0; k < minpts; k++) {
      QueryResult<DoubleDistance> qr = neighbors.get(k);
      Neighbor neighbor = new Neighbor(id, k, qr.getID(), 0, qr.getDistance().getDoubleValue());
      nnTable.insert(neighbor);
    }
  }

  /**
   * Computes the LOF value for a given object
   *
   * @param id       the object id
   */
  protected void computeLOF(Integer id) {
    NeighborList neighbors_o = nnTable.getNeighbors(id);

    double sum1 = 0;
    double[] sum2 = new double[minpts];

    for (int k = 0; k < neighbors_o.size(); k++) {
      Neighbor p = neighbors_o.get(k);

      // sum1
      sum1 += p.getReachabilityDistance();

      // sum2
      double sum = 0;
      NeighborList neighbors_p = nnTable.getNeighbors(p.getNeighborID());
      for (Neighbor q : neighbors_p) {
        sum += q.getReachabilityDistance();
      }
      sum2[k] = sum;
    }

    LOFEntry entry = new LOFEntry(sum1, sum2);
    lofTable.insert(id, entry);
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "LOF",
        "Local Outlier Factor",
        "Algorithm to compute density-based local outlier factors in a database based on the parameter "
        + MINPTS_P,
        "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander: "
        + " LOF: Identifying Density-Based Local Outliers. "
        + "In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), "
        + "Dallas, TX, 2000.");
  }

  /**
   * Sets the parameters minpts additionally to the parameters set by the
   * super-class method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // pagesize
    pageSize = (Integer) optionHandler.getOptionValue(PAGE_SIZE_P);

    // cachesize
    cacheSize = (Integer) optionHandler.getOptionValue(CACHE_SIZE_P);

    // minpts
    minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

    return attributeSettings;
  }

}
