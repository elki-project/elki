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
  @Override
  OutlierResult run(Database database) throws IllegalStateException;
}