package de.lmu.ifi.dbs.elki.result.outlier;

import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * Wrap a typical Outlier result, keeping direct references to the main result
 * parts.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.AnnotationResult oneway - - contains
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.result.outlier.OutlierOrderingResult
 */
public class OutlierResult extends BasicResult {
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
   */
  public OutlierResult(OutlierScoreMeta meta, AnnotationResult<Double> scores) {
    super(scores.getLongName(), scores.getShortName());
    this.meta = meta;
    this.scores = scores;
    this.ordering = new OutlierOrderingResult(scores, !(meta instanceof InvertedOutlierScoreMeta));
    this.addChildResult(scores);
    this.addChildResult(ordering);
    this.addChildResult(meta);
  }

  /**
   * Get the outlier score meta data
   * 
   * @return the outlier meta information
   */
  public OutlierScoreMeta getOutlierMeta() {
    return meta;
  }

  /**
   * Get the outlier scores association.
   * 
   * @return the scores
   */
  public AnnotationResult<Double> getScores() {
    return scores;
  }

  /**
   * Get the outlier ordering
   * 
   * @return the ordering
   */
  public OrderingResult getOrdering() {
    return ordering;
  }
}