package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DBOutlierScore(Parameterization config) {
    super(config);
  }

  @Override
  protected DataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D d) {
    double n;

    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      n = (database.rangeQuery(id, d, distFunc).size()) / (double) database.size();
      scores.put(id, 1 - n);
    }
    scores.toString();
    return scores;
  }
}