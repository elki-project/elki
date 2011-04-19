package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.result.ResultHandler;

/**
 * Interface for post-algorithm evaluations, such as histograms, outlier score
 * evaluations, ...
 * 
 * @author Erich Schubert
 */
public interface Evaluator extends ResultHandler {
  // Empty now - uses ResultHandler API. Remove?
}