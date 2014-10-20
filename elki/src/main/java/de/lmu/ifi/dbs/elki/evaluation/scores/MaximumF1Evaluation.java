package de.lmu.ifi.dbs.elki.evaluation.scores;

/**
 * Evaluate using the maximum F1 score.
 * 
 * @author Erich Schubert
 */
public class MaximumF1Evaluation extends AbstractScoreEvaluation {
  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    final int postot = predicate.numPositive();
    int poscnt = 0, cnt = 0;
    double maxf1 = 0.;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        ++cnt;
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // New F1 value:
      double p = poscnt / (double) cnt, r = poscnt / (double) postot;
      double f1 = 2. * p * r / (p + r);
      if(f1 > maxf1) {
        maxf1 = f1;
      }
    }
    return maxf1;
  }
}
