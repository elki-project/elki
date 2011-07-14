package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.result.ResultProcessor;

/**
 * Interface for post-algorithm evaluations, such as histograms, outlier score
 * evaluations, ...
 * 
 * @author Erich Schubert
 */
public interface Evaluator extends ResultProcessor {
  // Empty now - uses ResultProcessor API and serves merely UI purposes.
}