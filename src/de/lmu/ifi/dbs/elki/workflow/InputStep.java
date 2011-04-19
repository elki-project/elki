package de.lmu.ifi.dbs.elki.workflow;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Data input step of the workflow.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DatabaseConnection
 * @apiviz.has Database
 */
public class InputStep implements WorkflowStep {
  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection databaseConnection;

  /**
   * Database read.
   */
  private Database db = null;

  /**
   * Constructor.
   *
   * @param databaseConnection
   */
  public InputStep(DatabaseConnection databaseConnection) {
    super();
    this.databaseConnection = databaseConnection;
  }

  /**
   * Get the database to use.
   * 
   * @return Database
   */
  public Database getDatabase() {
    if (db == null) {
      db  = databaseConnection.getDatabase();
    }
    return db;
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the database connection to have the algorithm run with.
     */
    protected DatabaseConnection databaseConnection = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<DatabaseConnection> dbcP = new ObjectParameter<DatabaseConnection>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }      
    }

    @Override
    protected InputStep makeInstance() {
      return new InputStep(databaseConnection);
    }
  }
}