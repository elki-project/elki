package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.LOFResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts.
 *
 * @author Peer Kr&ouml;ger (<a href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */
public class LOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance> {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>positive number of nearest neighbors of an object to be considered for computing its LOF.";

  /**
   * Minimum points.
   */
  private int minpts;

  /**
   * Provides the result of the algorithm.
   */
  private LOFResult<O> result;

  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since LOF is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public LOF() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    try {
      getDistanceFunction().setDatabase(database, isVerbose(), isTime());
      if (isVerbose()) {
        logger.info("\n##### Computing LOFs:\n");
      }

      // compute neighbors of each db object
      if (isVerbose()) {
        logger.info("\nStep 1: computing neighborhoods:\n");
      }
      {
        Progress progressNeighborhoods = new Progress("LOF", database.size());
        int counter = 1;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
          Integer id = iter.next();
          computeNeighbors(database, id);
          if (isVerbose()) {
            progressNeighborhoods.setProcessed(counter);
            logger.log(new ProgressLogRecord(Level.INFO, Util.status(progressNeighborhoods), progressNeighborhoods.getTask(), progressNeighborhoods.status()));
          }
        }
        if (isVerbose()) {
          logger.info("\n");
        }
      }

      // compute local reachability density of each db object
      if (isVerbose()) {
        logger.info("\n Step 2: computing LRDs:\n");
      }
      {
        Progress progressLRDs = new Progress("LOF: LRDs", database.size());
        int counter = 1;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
          Integer id = iter.next();
          computeLRD(database, id);
          if (isVerbose()) {
            progressLRDs.setProcessed(counter);
            logger.log(new ProgressLogRecord(Level.INFO, Util.status(progressLRDs), progressLRDs.getTask(), progressLRDs.status()));
          }
        }
        if (isVerbose()) {
          logger.info("\n");
        }
      }

      // compute LOF of each db object
      if (isVerbose()) {
        logger.info("\n Step 3: finally, computing LOFs:\n");
      }
      // keeps the lofs for each object
      IDDoublePair[] resultArray = new IDDoublePair[database.size()];
      {
        Progress progressLOFs = new Progress("LOF: LOF for objects", database.size());
        int counter = 0;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
          Integer id = iter.next();
          resultArray[counter] = new IDDoublePair(id, computeLOF(database, id));
          if (isVerbose()) {
            progressLOFs.setProcessed(counter + 1);
            logger.log(new ProgressLogRecord(Level.INFO, Util.status(progressLOFs), progressLOFs.getTask(), progressLOFs.status()));
          }
        }
        if (isVerbose()) {
          logger.info("\n");
        }
      }

      result = new LOFResult<O>(database, resultArray);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * Computes the k-nearest neighbors of a given object in a given database.
   *
   * @param database the database on which the LRD is computed
   * @param objectID the object
   */
  public void computeNeighbors(Database<O> database, Integer objectID) {
    List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(objectID, minpts + 1, getDistanceFunction());
    neighbors.remove(0);
    database.associate(AssociationID.NEIGHBORS, objectID, neighbors);
  }

  /**
   * Computes the local reachability density (LRD) of a given object.
   *
   * @param database the database on which the LRD is computed
   * @param objectID the object
   */
  public void computeLRD(Database<O> database, Integer objectID) {
    List<QueryResult<DoubleDistance>> neighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, objectID);

    double sum = 0;
    for (QueryResult<DoubleDistance> o : neighbors) {
      Integer oID = o.getID();
      double oDist = o.getDistance().getDoubleValue();
      List<QueryResult<DoubleDistance>> neighborsO = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, oID);
      double oCoreDist = neighborsO.get(neighborsO.size() - 1).getDistance().getDoubleValue();
      double oReachDist = Math.max(oCoreDist, oDist);
      sum = sum + oReachDist;
    }

    double lrd = neighbors.size() / sum;
    database.associate(AssociationID.LRD, objectID, lrd);
  }

  /**
   * Computes the LOF value for a given object
   *
   * @param database the database on which the algorithm is run
   * @param objectID object the LOF of which is computed
   */
  protected Double computeLOF(Database<O> database, Integer objectID) {
    List<QueryResult<DoubleDistance>> neighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, objectID);
    Double pLRD = (Double) database.getAssociation(AssociationID.LRD, objectID);

    double sum = 0;
    for (QueryResult<DoubleDistance> o : neighbors) {
      Integer oID = o.getID();
      Double oLRD = (Double) database.getAssociation(AssociationID.LRD, oID);
      sum = sum + oLRD / pLRD;
    }
    sum = sum / neighbors.size();
    return sum;
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("LOF", "Local Outlier Factor",
                           "Algorithm to compute density-based local outlier factors in a database based on the parameter " + MINPTS_P,
                           "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander: " + " LOF: Identifying Density-Based Local Outliers. " + "In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), " + "Dallas, TX, 2000.");
  }

  /**
   * Sets the parameters minpts additionally to the parameters set
   * by the super-class method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0) {
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

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
