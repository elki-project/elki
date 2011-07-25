package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate.Factory;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Moran scatterplot outliers, based on the standardized deviation from the
 * local and global means. In contrast to the definition given in the reference,
 * we use this as a ranking outlier detection by not applying the signedness test,
 * but by using the score (- localZ) * (Average localZ of Neighborhood) directly.
 * This allows us to differentiate a bit between stronger and weaker outliers.
 * 
 * <p>
 * Reference: <br>
 * S. Shekhar and C.-T. Lu and P. Zhang <br>
 * A Unified Approach to Detecting Spatial Outliers <br>
 * in GeoInformatica 7-2, 2003
 * 
 * <p>
 * Moran scatterplot is a plot of normalized attribute values against the
 * neighborhood average of normalized attribute values. Spatial Objects on the
 * upper left or lower right are Spatial Outliers.
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood type
 */
@Title("Moran Scatterplot Outlier")
@Description("Spatial Outlier detection based on the standardized deviation from the local means.")
@Reference(authors = "S. Shekhar and C.-T. Lu and P. Zhang", title = "A Unified Approach to Detecting Spatial Outliers", booktitle = "GeoInformatica 7-2, 2003")
public class MoranScatterPlotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MoranScatterPlotOutlier.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood
   */
  public MoranScatterPlotOutlier(Factory<N> npredf) {
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

    // Compute the global mean and variance
    MeanVariance globalmv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      globalmv.put(relation.get(id).doubleValue(1));
    }

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);

    // calculate normalized attribute values
    // calculate neighborhood average of normalized attribute values.
    for(DBID id : relation.getDBIDs()) {
      // Compute global z score
      final double globalZ = (relation.get(id).doubleValue(1) - globalmv.getMean()) / globalmv.getNaiveStddev();
      // Compute local average z score
      Mean localm = new Mean();
      for(DBID n : npred.getNeighborDBIDs(id)) {
        if(id.equals(n)) {
          continue;
        }
        localm.put((relation.get(n).doubleValue(1) - globalmv.getMean()) / globalmv.getNaiveStddev());
      }
      // if neighors.size == 0
      final double localZ;
      if(localm.getCount() > 0) {
        localZ = localm.getMean();
      }
      else {
        // if s has no neighbors => Wzi = zi
        localZ = globalZ;
      }

      // compute score
      // Note: in the original moran scatterplot, any object with a score < 0 would be an outlier.
      final double score = Math.max(-globalZ * localZ, 0);
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("MoranOutlier", "Moran Scatterplot Outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
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
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected MoranScatterPlotOutlier<N> makeInstance() {
      return new MoranScatterPlotOutlier<N>(npredf);
    }
  }
}