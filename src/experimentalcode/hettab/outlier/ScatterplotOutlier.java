package experimentalcode.hettab.outlier;

import org.apache.commons.math.stat.regression.SimpleRegression;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;

/**
 * FIXME: Documentation, Reference
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood object type
 */
public class ScatterplotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ScatterplotOutlier.class);

  /**
   * The association id to associate the SCORE of an object for the algorithm.
   */
  public static final AssociationID<Double> SCATTERPLOT_SCORE = AssociationID.getOrCreateAssociationID("score", TypeUtil.DOUBLE);

  /**
   * Constructor
   * 
   * @param npredf
   */
  public ScatterplotOutlier(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  /**
   * Main method
   * 
   * @param database Database
   * @param nrel Neighborhood relation
   * @param relation Data relation (1d!)
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);
    WritableDataStore<Double> means = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);

    // calculate average for each object
    for(DBID id : relation.getDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double d = neighbors.size();
      double avg = 0;
      for(DBID n : neighbors) {
        avg += relation.get(n).doubleValue(1);
      }
      means.put(id, avg / d);
    }

    // calculate errors using regression
    SimpleRegression regression = new SimpleRegression();
    for(DBID id : relation.getDBIDs()) {
      regression.addData(relation.get(id).doubleValue(1), means.get(id));
    }

    // calculate mean and variance for error
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      double y_i = relation.get(id).doubleValue(1);
      double e = means.get(id) - (regression.getSlope() * y_i + regression.getIntercept());
      error.put(id, e);
      mv.put(e);
    }

    double mean = mv.getMean();
    double variance = mv.getSampleVariance();

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((error.get(id) - mean) / variance);
      minmax.put(score);
      scores.put(id, score);

    }
    // build representation
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SPO", "Scatterplot-Outlier", SCATTERPLOT_SCORE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  /**
   * Parameterization class
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighbordhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected ScatterplotOutlier<N> makeInstance() {
      return new ScatterplotOutlier<N>(npredf);
    }
  }
}