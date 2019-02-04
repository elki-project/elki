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
package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Pseudo database that is empty.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Title("Empty Database")
@Description("Dummy data source that does not provide any objects.")
@Alias("de.lmu.ifi.dbs.elki.database.connection.EmptyDatabaseConnection")
public class EmptyDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Static logger
   */
  private static final Logging LOG = Logging.getLogger(EmptyDatabaseConnection.class);
  
  /**
   * Constructor.
   */
  public EmptyDatabaseConnection() {
    super(null);
  }
  
  @Override
  public MultipleObjectsBundle loadData() {
    // Return an empty bundle
    // TODO: add some dummy column, such as DBIDs?
    return new MultipleObjectsBundle();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
