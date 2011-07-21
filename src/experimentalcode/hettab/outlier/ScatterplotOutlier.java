package experimentalcode.hettab.outlier;

import org.apache.commons.math.stat.regression.SimpleRegression;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Scatterplot-Outlier
 * <p>
 * Reference: <br>
 * S. Shekhar and C.-T. Lu and P. Zhang <br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * in in GeoInformatica 7-2, 2003.
 * </p>
 * 
 * <p>
 * Scatterplot shows attribute values on the X-axis and the average of the
 * attribute values in the neighborhood on the Y-axis. Best fit regression line
 * is used to identify spatial outliers. Vertical difference of a data point
 * tells about outlierness.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood object type
 */
@Title("A Unified Approach to Spatial Outliers Detection")
@Description("Spatial Outlier Detection Algorithm")
@Reference(authors = "S. Shekhar and C.-T. Lu and P. Zhang", title = "A Unified Approach to Detecting Spatial Outliers", booktitle = "GeoInformatica 7-2, 2003")
public class ScatterplotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ScatterplotOutlier.class);

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
   * @param nrel Neighborhood relation
   * @param relation Data relation (1d!)
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);
    WritableDataStore<Double> means = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);

    // calculate average for each object
    // if object id has no neighbors ==> avg = non-spatial attribut of id
    for(DBID id : relation.getDBIDs()) {
      int cnt = 0 ;
      double avg = 0 ;
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      for(DBID n : neighbors) {
        if(id.equals(n)){
          continue ;
        }
        avg += relation.get(n).doubleValue(1);
        cnt++ ;
      }
      if(cnt>0){
        means.put(id,  avg/cnt) ;
      }
      else{
        means.put(id,relation.get(id).doubleValue(1));
      }
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
    double variance = mv.getNaiveStddev();

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((error.get(id) - mean) / variance);
      minmax.put(score);
      scores.put(id, score);

    }
    // build representation
    Relation<Double> scoreResult = new MaterializedRelation<Double>("SPO", "Scatterplot-Outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
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