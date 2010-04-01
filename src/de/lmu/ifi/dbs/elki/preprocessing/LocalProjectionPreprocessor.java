package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;

/**
 * Preprocessor returning a local projection result (e.g. a PCA result or a subspace selection result)
 * 
 * @author Erich Schubert
 *
 * @param <V> Object type
 * @param <R> Projection type
 */
public interface LocalProjectionPreprocessor<V extends DatabaseObject, R extends ProjectionResult> extends Preprocessor<V, R> {
  // Empty marker interface
}
