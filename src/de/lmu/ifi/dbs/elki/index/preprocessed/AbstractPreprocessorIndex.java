package de.lmu.ifi.dbs.elki.index.preprocessed;

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for simple preprocessor based indexes, requiring a simple
 * object storage for preprocessing results.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <R> Stored data type
 */
public abstract class AbstractPreprocessorIndex<O, R> extends AbstractIndex<O> {
  /**
   * The data store
   */
  protected WritableDataStore<R> storage;

  /**
   * Constructor.
   */
  public AbstractPreprocessorIndex(Relation<O> relation) {
    super(relation);
  }

  /**
   * Get the classes static logger.
   * 
   * @return Logger
   */
  abstract protected Logging getLogger();
}
