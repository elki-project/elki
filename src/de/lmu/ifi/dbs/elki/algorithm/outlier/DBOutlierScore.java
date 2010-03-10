package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.Description;
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
public class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends AbstractDBOutlier<O, D> {
  /**
   * Constructor.
   * 
   * @param config Parameterization
   */
  public DBOutlierScore(Parameterization config) {
    super(config);
  }

  @Override
  protected HashMap<Integer, Double> computeOutlierScores(Database<O> database, D d) {
    double n;

    HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
    for(Integer id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      n = (database.rangeQuery(id, d, getDistanceFunction()).size()) / (double) database.size();
      scores.put(id, 1 - n);
    }
    scores.toString();
    return scores;
  }

  @Override
  public Description getDescription() {
    return new Description("DB Outlier Score", "Distance based outlier score", "Generalization of the original DB-Outler approach to a ranking method, by turning the fraction parameter into the output value.", "Gernalization of a method published in E.M. Knorr, R. T. Ng: Algorithms for Mining Distance-Based Outliers in Large Datasets, In: Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998.");
  }
}