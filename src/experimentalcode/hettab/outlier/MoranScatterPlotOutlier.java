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
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * <p>
 * Reference: <br>
 * S. Shekhar and C.-T. Lu and P. Zhang <br>
 * A Unified Approach to Detecting Spatial Outliers <br>
 * in GeoInformatica 7-2, 2003
 * </p>
 * <p>
 * Moran scatterplot is a plot of normalized attribute values against the
 * neighborhood average of normalized attribute values. Spatial Objects on the
 * upper left or lower right are Spatial Outliers.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood type
 */
@Title("A Unified Approach to Spatial Outliers Detection")
@Description("Spatial Outlier Detection Algorithm")
@Reference(authors = "S. Shekhar and C.-T. Lu and P. Zhang", title = "A Unified Approach to Detecting Spatial Outliers", booktitle = "GeoInformatica 7-2, 2003")
public class MoranScatterPlotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MoranScatterPlotOutlier.class);

  /**
   * 
   * The association id to associate the SCORE of an object for the
   * MoranScatterplotOutlier algorithm.
   */
  public static final AssociationID<Double> SCORE = AssociationID.getOrCreateAssociationID("Moran-Outlier", TypeUtil.DOUBLE);

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

    // add non-spatial value to MeanVariance
    MeanVariance stdZMV = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      stdZMV.put(relation.get(id).doubleValue(1));
    }

    // calculate normalized attribute values
    // calculate neighborhood average of normalized attribute values.
    for(DBID id : relation.getDBIDs()) {
      double zValue = (relation.get(id).doubleValue(1) - stdZMV.getMean()) / stdZMV.getNaiveStddev();
      stdZ.put(id, zValue);
      double neighborZValue = 0;
      for(DBID n : npred.getNeighborDBIDs(id)) {
        neighborZValue += (relation.get(n).doubleValue(1) - stdZMV.getMean()) / stdZMV.getNaiveStddev();
      }
      neighborStdZ.put(id, neighborZValue / npred.getNeighborDBIDs(id).size());
    }

    // compute score
    // Spatial Object with score=1 are Spatial Outlier
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = stdZ.get(id) * neighborStdZ.get(id);
      if(score < 0) {
        minmax.put(1.0);
        scores.put(id, 1.0);
      }
      else {
        minmax.put(0.0);
        scores.put(id, 0.0);
      }
    }

    Relation<Double> scoreResult = new AnnotationFromDataStore<Double>("MoranOutlier", "Moran Scatterplot Outlier", SCORE, scores, relation.getDBIDs());
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