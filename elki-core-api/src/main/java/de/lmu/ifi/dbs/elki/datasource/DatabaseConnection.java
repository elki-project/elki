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

/**
 * DatabaseConnection is used to load data into a database.
 * <p>
 * A database connection is to manage the input and for a database where
 * algorithms can run on. An implementation may either use a parser to parse a
 * sequential file or piped input and provide a file based database or provide
 * an intermediate connection to a database system.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @opt nodefillcolor LemonChiffon
 * @has - - - MultipleObjectsBundle
 */
public interface DatabaseConnection {
  /**
   * Returns the initial data for a database.
   * 
   * @return a database object bundle
   */
  // TODO: streaming load?
  MultipleObjectsBundle loadData();
}