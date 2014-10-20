package de.lmu.ifi.dbs.elki.evaluation.scores;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Evaluate using average precision.
 * 
 * @author Erich Schubert
 */
public class AveragePrecisionEvaluation extends AbstractScoreEvaluation {
  /**
   * Static instance
   */
  public static final AveragePrecisionEvaluation STATIC = new AveragePrecisionEvaluation();

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0, pospre = 0;
    double acc = 0.;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // Add a new point.
      if(poscnt > pospre) {
        acc += (poscnt / (double) (poscnt + negcnt)) * (poscnt - pospre);
        pospre = poscnt;
      }
    }
    return (poscnt > 0) ? acc / poscnt : 0.;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected AveragePrecisionEvaluation makeInstance() {
      return STATIC;
    }
  }
}
