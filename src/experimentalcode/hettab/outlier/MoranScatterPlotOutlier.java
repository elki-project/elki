package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate.Factory;
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
 * @author hettab
 * 
 * @param <N> Neighborhood type
 */
public class MoranScatterPlotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MoranScatterPlotOutlier.class);

  /**
   * FIXME: Documentation
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SCORE = AssociationID.getOrCreateAssociationID("Moran-Outlier", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   */
  public MoranScatterPlotOutlier(Factory<N> npredf) {
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
    WritableDataStore<Double> stdZ = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> neighborStdZ = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);
    MeanVariance stdZMV = new MeanVariance();

    for(DBID id : relation.getDBIDs()) {
      stdZMV.put(relation.get(id).doubleValue(1));
    }

    //System.out.println(stdZMV.getMean());
    //System.out.println(stdZMV.getSampleVariance());

    for(DBID id : relation.getDBIDs()) {
      double zValue = (relation.get(id).doubleValue(1) - stdZMV.getMean()) / stdZMV.getSampleVariance();
      stdZ.put(id, zValue);
      double neighborZValue = 0;
      for(DBID n : npred.getNeighborDBIDs(id)) {
        neighborZValue += (relation.get(n).doubleValue(1) - stdZMV.getMean()) / stdZMV.getSampleVariance();
      }
      neighborStdZ.put(id, neighborZValue / npred.getNeighborDBIDs(id).size());
    }

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = stdZ.get(id) * neighborStdZ.get(id);
      // System.out.println(score);
      if(score < 0) {
        minmax.put(1.0);
        scores.put(id, 1.0);
      }
      else {
        minmax.put(0.0);
        scores.put(id, 0.0);
      }
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("MoranOutlier", "Moran Scatterplot Outlier", SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
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
    protected MoranScatterPlotOutlier<N> makeInstance() {
      return new MoranScatterPlotOutlier<N>(npredf);
    }
  }
}