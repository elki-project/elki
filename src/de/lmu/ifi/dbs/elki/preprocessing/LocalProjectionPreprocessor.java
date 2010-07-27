package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Preprocessor returning a local projection result (e.g. a PCA result or a
 * subspace selection result)
 * 
 * @author Erich Schubert
 * 
 * @param <R> Projection type
 */
public interface LocalProjectionPreprocessor<O extends DatabaseObject, R extends ProjectionResult> extends Preprocessor<O, R>, Parameterizable {
  /**
   * This method executes the particular preprocessing step of this Preprocessor
   * for the objects of the specified database.
   * 
   * @param database the database for which the preprocessing is performed
   */
  public <T extends O> Instance<R> instantiate(Database<T> database);

  /**
   * Instance interface.
   * 
   * @author Erich Schubert
   * 
   * @param <R> projection result type
   */
  public static interface Instance<R extends ProjectionResult> extends Preprocessor.Instance<R> {
    // Empty interface
  }
}
