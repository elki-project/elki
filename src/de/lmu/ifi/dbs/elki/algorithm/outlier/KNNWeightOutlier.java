package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier Detection based on the accumulated distances of a point to its k
 * nearest neighbors.
 * 
 * Based on: F. Angiulli, C. Pizzuti: Fast Outlier Detection in High Dimensional
 * Spaces. In: Proc. European Conference on Principles of Knowledge Discovery
 * and Data Mining (PKDD'02), Helsinki, Finland, 2002.
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier Detection based on the distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", title = "Fast Outlier Detection in High Dimensional Spaces", booktitle = "Proc. European Conference on Principles of Knowledge Discovery and Data Mining (PKDD'02), Helsinki, Finland, 2002", url = "http://dx.doi.org/10.1007/3-540-45681-3_2")
public class KNNWeightOutlier<O extends DatabaseObject, D extends DoubleDistance> extends DistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnwod.k", "k nearest neighbor");

  /**
   * Association ID for the KNN Weight Outlier Detection
   */
  public static final AssociationID<Double> KNNWOD_WEIGHT = AssociationID.getOrCreateAssociationID("knnwod_weight", Double.class);

  /**
   * Parameter to specify the k nearest neighbor,
   * 
   * <p>
   * Key: {@code -knnwod.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * Constructor, adding options to option handler.
   */
  public KNNWeightOutlier(Parameterization config) {
    super(config);
    // k nearest neighbor
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double maxweight = 0;
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    if(this.isVerbose()) {
      this.verbose("computing outlier degree(sum of the distances to the k nearest neighbors");
    }
    FiniteProgress progressKNNWeight = new FiniteProgress("KNNWOD_KNNWEIGHT for objects", database.size());
    int counter = 0;

    // compute distance to the k nearest neighbor. n objects with the highest
    // distance are flagged as outliers
    HashMap<Integer, Double> knnw_score = new HashMap<Integer, Double>(database.size());
    for(Integer id : database) {
      counter++;
      // compute sum of the distances to the k nearest neighbors

      List<DistanceResultPair<DoubleDistance>> knn = database.kNNQueryForID(id, k, getDistanceFunction());
      DoubleDistance skn = knn.get(0).getFirst();
      for(int i = 1; i < k; i++) {
        skn = skn.plus(knn.get(i).getFirst());
      }

      double doubleSkn = skn.getValue();
      if(doubleSkn > maxweight) {
        maxweight = doubleSkn;
      }
      knnw_score.put(id, doubleSkn);

      if(this.isVerbose()) {
        progressKNNWeight.setProcessed(counter);
        this.progress(progressKNNWeight);
      }
    }

    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(KNNWOD_WEIGHT, knnw_score);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(knnw_score, true);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxweight, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, res1, res2);
  }
}