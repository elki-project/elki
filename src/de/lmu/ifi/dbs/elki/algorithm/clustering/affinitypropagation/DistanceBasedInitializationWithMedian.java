package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance based initialization.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class DistanceBasedInitializationWithMedian<O, D extends NumberDistance<D, ?>> implements AffinityPropagationInitialization<O> {
  /**
   * Distance function.
   */
  DistanceFunction<? super O, D> distance;

  /**
   * Quantile to use.
   */
  double quantile;

  /**
   * Constructor.
   * 
   * @param distance Similarity function
   * @param quantile Quantile
   */
  public DistanceBasedInitializationWithMedian(DistanceFunction<? super O, D> distance, double quantile) {
    super();
    this.distance = distance;
    this.quantile = quantile;
  }

  @Override
  public double[][] getSimilarityMatrix(Database db, Relation<O> relation, ArrayDBIDs ids) {
    final int size = ids.size();
    DistanceQuery<O, D> dq = db.getDistanceQuery(relation, distance);
    double[][] mat = new double[size][size];
    double[] flat = new double[(size * (size - 1)) >> 1];
    // TODO: optimize for double valued primitive distances.
    DBIDArrayIter i1 = ids.iter(), i2 = ids.iter();
    for(int i = 0, j = 0; i < size; i++, i1.advance()) {
      double[] mati = mat[i];
      i2.seek(i + 1);
      for(int k = i + 1; k < size; k++, i2.advance()) {
        mati[k] = -dq.distance(i1, i2).doubleValue();
        mat[k][i] = mati[k]; // symmetry.
        flat[j] = mati[k];
        j++;
      }
    }
    double median = QuickSelect.quantile(flat, quantile);
    // On the diagonal, we place the median
    for(int i = 0; i < size; i++) {
      mat[i][i] = median;
    }
    return mat;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distance.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractParameterizer {
    /**
     * Parameter for the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("ap.distance", "Distance function to use.");

    /**
     * istance function.
     */
    DistanceFunction<? super O, D> distance;

    /**
     * Quantile to use.
     */
    double quantile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O, D>> param = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(param)) {
        distance = param.instantiateClass(config);
      }

      DoubleParameter quantileP = new DoubleParameter(QUANTILE_ID, .5);
      if(config.grab(quantileP)) {
        quantile = quantileP.doubleValue();
      }
    }

    @Override
    protected DistanceBasedInitializationWithMedian<O, D> makeInstance() {
      return new DistanceBasedInitializationWithMedian<>(distance, quantile);
    }
  }
}