package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

/**
 * Generic super interface for outlier detection algorithms.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has OutlierResult
 */
public interface OutlierAlgorithm extends Algorithm {
  // Note: usually you won't override this method directly, but instead
  // Use the magic in AbstractAlgorithm and just implement a run method for your input data
  @Override
  OutlierResult run(Database database) throws IllegalStateException;
}