package de.lmu.ifi.dbs.elki.workflow;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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