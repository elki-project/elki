package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Compute percentage of neighbors in the given neighborhood with size d.
 * 
 * Generalization of the DB Outlier Detection by using the fraction as outlier
 * score thus eliminating this parameter and turning the method into a ranking
 * method instead of a labelling one.
 * 
 * @author Lisa Reichert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
@Title("Distance based outlier score")
@Description("Generalization of the original DB-Outlier approach to a ranking method, by turning the fraction parameter into the output value.")
@Reference(prefix = "Generalization of a method proposed in", authors = "E.M. Knorr, R. T. Ng", title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", booktitle = "Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998")
public class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends AbstractDBOutlier<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBOutlierScore.class);
  
  /**
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param d distance radius parameter
   */
  public DBOutlierScore(DistanceFunction<O, D> distanceFunction, D d) {
    super(distanceFunction, d);
  }

  @Override
  protected DataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D d) {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    RangeQuery<O, D> rangeQuery = database.getRangeQuery(distFunc);
    // TODO: use bulk when implemented.
    for(DBID id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      double n = (rangeQuery.getRangeForDBID(id, d).size()) / (double) database.size();
      scores.put(id, 1.0 - n);
    }
    scores.toString();
    return scores;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return ABOD Algorithm
   */
  public static <O extends DatabaseObject, D extends Distance<D>> DBOutlierScore<O, D> parameterize(Parameterization config) {
    // distance used in preprocessor
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    // d parameter
    D d = getParameterD(config, distanceFunction);

    if(config.hasErrors()) {
      return null;
    }
    return new DBOutlierScore<O, D>(distanceFunction, d);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}