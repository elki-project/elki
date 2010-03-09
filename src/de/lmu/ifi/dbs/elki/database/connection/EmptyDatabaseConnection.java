package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo database that is empty. Ugly hack.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class EmptyDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> implements Parameterizable {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public EmptyDatabaseConnection(Parameterization config) {
    super(config, false);
  }

  /**
   * @param normalization ignored for an empty database.
   */
  @Override
  public Database<O> getDatabase(Normalization<O> normalization) {
    return database;
  }

  @Override
  public Description getDescription() {
    return new Description(this.getClass().getName(), "Empty Database", "Dummy database that does not contain any objects.");
  }
}