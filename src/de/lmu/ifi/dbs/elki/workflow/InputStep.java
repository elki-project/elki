package de.lmu.ifi.dbs.elki.workflow;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Data input step of the workflow.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DatabaseConnection
 * @apiviz.has Database
 *
 * @param <O> Database object type
 */
public class InputStep<O extends DatabaseObject> implements WorkflowStep {
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
   * Constructor.
   *
   * @param databaseConnection
   * @param normalization
   * @param normalizationUndo
   */
  public InputStep(DatabaseConnection<O> databaseConnection, Normalization<O> normalization, boolean normalizationUndo) {
    super();
    this.databaseConnection = databaseConnection;
    this.normalization = normalization;
    this.normalizationUndo = normalizationUndo;
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
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractParameterizer {
    /**
     * Holds the database connection to have the algorithm run with.
     */
    protected DatabaseConnection<O> databaseConnection = null;

    /**
     * A normalization - per default no normalization is used.
     */
    protected Normalization<O> normalization = null;

    /**
     * Whether to undo normalization for result.
     */
    protected boolean normalizationUndo = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<DatabaseConnection<O>> dbcP = new ObjectParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }
      
      // parameter normalization
      final ObjectParameter<Normalization<O>> normP = new ObjectParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);
      final Flag normUndoF = new Flag(OptionID.NORMALIZATION_UNDO);
      config.grab(normP);
      config.grab(normUndoF);
      // normalization-undo depends on a defined normalization.
      GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Class<?>, Class<? extends Normalization<O>>>(normP, null, normUndoF, true);
      config.checkConstraint(gpc);
      if(normP.isDefined()) {
        normalization = normP.instantiateClass(config);
        normalizationUndo = normUndoF.getValue();
      }
    }

    @Override
    protected InputStep<O> makeInstance() {
      return new InputStep<O>(databaseConnection, normalization, normalizationUndo);
    }
  }
}