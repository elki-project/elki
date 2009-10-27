package de.lmu.ifi.dbs.elki.result.outlier;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;

/**
 * Wrap a typical Outlier result, keeping direct references to the main result parts.
 * 
 * @author Erich Schubert
 */
public class OutlierResult extends MultiResult {
  /**
   * Constant association ID for the outlier score meta data.
   */
  private static final AssociationID<OutlierScoreMeta> OUTLIER_SCORE_META = AssociationID.getOrCreateAssociationID("OUTLIER_SCORE_META", OutlierScoreMeta.class);

  /**
   * Outlier score meta information
   */
  private OutlierScoreMeta meta;
  
  /**
   * Outlier scores.
   */
  private AnnotationResult<Double> scores;
  
  /**
   * Outlier ordering.
   */
  private OrderingResult ordering;
  
  /**
   * Constructor.
   * 
   * @param meta Outlier score metadata.
   * @param scores Scores result.
   * @param ordering Ordering result.
   */
  public OutlierResult(OutlierScoreMeta meta, AnnotationResult<Double> scores, OrderingResult ordering) {
    this.meta = meta;
    this.setAssociation(OUTLIER_SCORE_META, meta);
    this.scores = scores;
    this.addResult(scores);
    this.ordering = ordering;
    this.addResult(ordering);
  }

  /**
   * Get the outlier score meta data
   * @return the outlier meta information
   */
  protected OutlierScoreMeta getOutlierMeta() {
    return meta;
  }

  /**
   * Get the outlier scores association.
   * @return the scores
   */
  protected AnnotationResult<Double> getScores() {
    return scores;
  }

  /**
   * Get the outlier ordering
   * @return the ordering
   */
  protected OrderingResult getOrdering() {
    return ordering;
  }
}
