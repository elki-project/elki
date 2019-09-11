/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.workflow;

import elki.application.AbstractApplication;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Data input step of the workflow.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - Database
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
   */
  public static class Par implements Parameterizer {
    /**
     * Holds the database to have the algorithms run on.
     */
    protected Database database = null;

    /**
     * Option ID to specify the database type
     */
    public static final OptionID DATABASE_ID = AbstractApplication.Par.DATABASE_ID;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Database>(DATABASE_ID, Database.class, StaticArrayDatabase.class) //
          .grab(config, x -> database = x);
    }

    @Override
    public InputStep make() {
      return new InputStep(database);
    }
  }
}
