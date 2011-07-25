package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**<p>
 * Reference: <br>
 * Chang-Tien Lu <br>
 * Algorithms for Spatial Outlier Detection <br>
 * in Third IEEE International Conference on Data Mining <br>
 * </p>
 * 
 *Description: <br>
 * Median Algorithm uses Median to represent the average non-spatial attribute
 * value of neighbors. <br>
 * The Difference e = non-spatial-Attribut-Value - Median (Neighborhood) is
 * computed.<br>
 * The Spatial Objects with the highest standarized e value are Spatial
 * Outliers.
 * </p>
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood type
 */
@Title("Algorithms for Spatial Outlier Detection")
@Description("Spatial Outlier Detection Algorithm")
@Reference(authors = "Chang-Tien Lu", title = "Algorithms for Spatial Outlier Detection", booktitle = "Proceedings of the Third IEEE International Conference on Data Mining")
public class MedianAlgorithm<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MedianAlgorithm.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public MedianAlgorithm(NeighborSetPredicate.Factory<N> npredf) {
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
    WritableDataStore<Double> gi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> hi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    //
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      //
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      ArrayList<Double> fi = new ArrayList<Double>();
      // calculate and store Median of neighborhood
      for(DBID n : neighbors) {
        if(id.equals(n)){
          continue ;
        }
        fi.add( relation.get(n).doubleValue(1));
      }
      
      if(fi.size()>0){
      double median = getMean(fi);
      gi.put(id, median);
      double h = relation.get(id).doubleValue(1) - median;
      hi.put(id, h);
      mv.put(h);
      }
      else{
        gi.put(id,relation.get(id).doubleValue(1));
        hi.put(id,0.0);
        mv.put(0.0);
      }
    }

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((hi.get(id) - mv.getMean()) / mv.getNaiveStddev());
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("MOF", "Median-single-attribut-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }
  
  /**
   * 
   * @param values
   * @return
   */
  public static double getMean(ArrayList<Double> values){
    Collections.sort(values);
    int middle = values.size() / 2;
    if(values.size() % 2 == 1) {

      return values.get(middle);
    }
    else {

      return (values.get(middle - 1) + values.get(middle)) / 2.0;
    }
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
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected MedianAlgorithm<N> makeInstance() {
      return new MedianAlgorithm<N>(npredf);
    }
  }
}