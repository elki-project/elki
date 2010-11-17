package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Simple distanced based outlier detection algorithm. User has to specify two
 * parameters An object is flagged as an outlier if at least a fraction p of all
 * data objects has a distance aboce d from c
 * <p>
 * Reference: E.M. Knorr, R. T. Ng: Algorithms for Mining Distance-Based
 * Outliers in Large Datasets, In: Procs Int. Conf. on Very Large Databases
 * (VLDB'98), New York, USA, 1998.
 * 
 * This paper presents several Distance Based Outlier Detection algorithms.
 * Implemented here is a simple index based algorithm as presented in section
 * 3.1.
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("DBOD: Distance Based Outlier Detection")
@Description("If the D-neighborhood of an object contains only very few objects (less than (1-p) percent of the data) this object is flagged as an outlier")
@Reference(authors = "E.M. Knorr, R. T. Ng", title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", booktitle = "Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998")
public class DBOutlierDetection<O extends DatabaseObject, D extends Distance<D>> extends AbstractDBOutlier<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBOutlierDetection.class);

  /**
   * Parameter to specify the minimum fraction of objects that must be outside
   * the D- neighborhood of an outlier
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("dbod.p", "minimum fraction of objects that must be outside the D-neighborhood of an outlier");

  /**
   * Holds the value of {@link #P_ID}.
   */
  private double p;

  /**
   * Constructor with actual parameters.
   * 
   * @param distanceFunction distance function parameter
   * @param d distance query radius
   * @param p percentage parameter
   */
  public DBOutlierDetection(DistanceFunction<O, D> distanceFunction, D d, double p) {
    super(distanceFunction, d);
    this.p = p;
  }

  @Override
  protected DataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D neighborhoodSize) {
    // maximum number of objects in the D-neighborhood of an outlier
    int m = (int) ((database.size()) * (1 - p));

    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    if(logger.isVerbose()) {
      logger.verbose("computing outlier flag");
    }

    FiniteProgress progressOFlags = logger.isVerbose() ? new FiniteProgress("DBOutlier for objects", database.size(), logger) : null;
    int counter = 0;
    // if index exists, kNN query. if the distance to the mth nearest neighbor
    // is more than d -> object is outlier
    KNNQuery.Instance<O, D> knnQuery = database.getKNNQuery(distFunc, m, KNNQuery.HINT_OPTIMIZED_ONLY);
    if(knnQuery != null) {
      for(DBID id : database) {
        counter++;
        final List<DistanceResultPair<D>> knns = knnQuery.getKNNForDBID(id, m);
        if(logger.isDebugging()) {
          logger.debugFine("distance to mth nearest neighbour" + knns.toString());
        }
        if(knns.get(Math.min(m, knns.size()) - 1).getFirst().compareTo(neighborhoodSize) <= 0) {
          // flag as outlier
          scores.put(id, 1.0);
        }
        else {
          // flag as no outlier
          scores.put(id, 0.0);
        }
      }
      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, logger);
      }
    }
    else {
      // range query for each object. stop if m objects are found
      for(DBID id : database) {
        counter++;
        Iterator<DBID> iterator = database.iterator();
        int count = 0;
        while(iterator.hasNext() && count < m) {
          DBID currentID = iterator.next();
          D currentDistance = distFunc.distance(id, currentID);

          if(currentDistance.compareTo(neighborhoodSize) <= 0) {
            count++;
          }
        }

        if(count < m) {
          // flag as outlier
          scores.put(id, 1.0);
        }
        else {
          // flag as no outlier
          scores.put(id, 0.0);
        }
      }

      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, logger);
      }
    }
    if(progressOFlags != null) {
      progressOFlags.ensureCompleted(logger);
    }
    return scores;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return ABOD Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> DBOutlierDetection<O, D> parameterize(Parameterization config) {
    // distance used in preprocessor
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    // d parameter
    D d = getParameterD(config, distanceFunction);
    // p Parameter
    double p = getParameterP(config);

    if(config.hasErrors()) {
      return null;
    }
    return new DBOutlierDetection<O, D>(distanceFunction, d, p);
  }

  /**
   * Grab the 'p' configuration option.
   * 
   * @param config Parameterization
   * @return p Parameter
   */
  protected static double getParameterP(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(P_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return Double.NaN;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}