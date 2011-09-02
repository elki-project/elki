package de.lmu.ifi.dbs.elki.datasource;

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

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is used to load data into a database.
 * <p/>
 * A database connection is to manage the input and for a database where
 * algorithms can run on. An implementation may either use a parser to parse a
 * sequential file or piped input and provide a file based database or provide
 * an intermediate connection to a database system.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has MultipleObjectsBundle
 */
public interface DatabaseConnection extends Parameterizable {
  /**
   * Returns the initial data for a database.
   * 
   * @return a database object bundle
   */
  // TODO: streaming load?
  MultipleObjectsBundle loadData();
}