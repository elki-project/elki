package de.lmu.ifi.dbs.elki.workflow;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Data input step of the workflow.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Database
 */
public class InputStep implements WorkflowStep {
  /**
   * Holds the database to have the algorithms run with.
   */
  private Database database;

  /**
   * Constructor.
   *
   * @param database Database to use
   */
  public InputStep(Database database) {
    super();
    this.database = database;
  }

  /**
   * Get the database to use.
   * 
   * @return Database
   */
  public Database getDatabase() {
    database.initialize();
    return database;
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
     * Holds the database to have the algorithms run on.
     */
    protected Database database = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<Database> dbP = new ObjectParameter<Database>(OptionID.DATABASE, Database.class, StaticArrayDatabase.class);
      if(config.grab(dbP)) {
        database = dbP.instantiateClass(config);
      }      
    }

    @Override
    protected InputStep makeInstance() {
      return new InputStep(database);
    }
  }
}