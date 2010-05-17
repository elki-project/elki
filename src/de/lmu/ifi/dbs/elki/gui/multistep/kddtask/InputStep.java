package de.lmu.ifi.dbs.elki.gui.multistep.kddtask;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

public class InputStep<O extends DatabaseObject> implements Parameterizable {
  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * A normalization - per default no normalization is used.
   */
  private Normalization<O> normalization = null;

  /**
   * Whether to undo normalization for result.
   */
  private boolean normalizationUndo = false;

  /**
   * Database read.
   */
  private Database<O> db = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public InputStep(Parameterization config) {
    super();
    final ObjectParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ObjectParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
    final ObjectParameter<Normalization<O>> NORMALIZATION_PARAM = new ObjectParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);
    final Flag NORMALIZATION_UNDO_FLAG = new Flag(OptionID.NORMALIZATION_UNDO);
    if(config.grab(DATABASE_CONNECTION_PARAM)) {
      databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass(config);
    }
    // parameter normalization
    config.grab(NORMALIZATION_PARAM);
    config.grab(NORMALIZATION_UNDO_FLAG);
    // normalization-undo depends on a defined normalization.
    GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Class<?>, Class<? extends Normalization<O>>>(NORMALIZATION_PARAM, null, NORMALIZATION_UNDO_FLAG, true);
    config.checkConstraint(gpc);
    if(NORMALIZATION_PARAM.isDefined()) {
      normalization = NORMALIZATION_PARAM.instantiateClass(config);
      normalizationUndo = NORMALIZATION_UNDO_FLAG.getValue();
    }
  }
  
  /**
   * Get the database to use.
   * 
   * @return Database
   */
  public Database<O> getDatabase() {
    if (db == null) {
      db  = databaseConnection.getDatabase(normalization);
    }
    return db;
  }
  
  /**
   * Get the normalization classes
   * 
   * @return normalization class or {@code null}
   */
  public Normalization<O> getNormalization() {
    return normalization;
  }
  
  /**
   * Getter for normalizationUndo flag.
   * 
   * @return true when normalization should be reverted for output
   */
  public boolean getNormalizationUndo() {
    return normalizationUndo;
  }
}
