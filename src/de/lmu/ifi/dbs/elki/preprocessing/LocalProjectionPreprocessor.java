package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Preprocessor returning a local projection result (e.g. a PCA result or a subspace selection result)
 * 
 * @author Erich Schubert
 *
 * @param <R> Projection type
 */
public interface LocalProjectionPreprocessor<O extends DatabaseObject, R extends ProjectionResult> extends Preprocessor<O, R>, Parameterizable {
  // Empty marker interface
}
