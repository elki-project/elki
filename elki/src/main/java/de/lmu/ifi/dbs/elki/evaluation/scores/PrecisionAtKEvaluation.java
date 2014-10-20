package de.lmu.ifi.dbs.elki.evaluation.scores;

/**
 * Evaluate using Precision@k.
 *
 * @author Erich Schubert
 */
public class PrecisionAtKEvaluation extends AbstractScoreEvaluation {
  /**
   * Parameter k.
   */
  int k;

  /**
   * Constructor.
   *
   * @param k k to evaluate at.
   */
  public PrecisionAtKEvaluation(int k) {
    this.k = k;
  }

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    int total = 0;
    double score = 0.;
    while(iter.valid() && total < k) {
      int posthis = 0, cntthis = 0;
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++posthis;
        }
        ++cntthis;
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // Special tie computations only when we reach k.
      if(total + cntthis > k) {
        // p = posthis / cntthis chance of being positive
        // n = (k-total) draws.
        // expected value = n * p
        score += posthis / (double) cntthis * (k - total);
        total = k;
        break;
      }
      score += posthis;
      total += cntthis;
    }
    return score / total;
  }

}
