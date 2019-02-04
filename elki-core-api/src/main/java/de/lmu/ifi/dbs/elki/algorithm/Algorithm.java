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
package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Specifies the requirements for any algorithm that is to be executable by the
 * main class.
 * <p>
 * Any implementation needs not to take care of input nor output, parsing and so
 * on. Those tasks are performed by the framework. An algorithm simply needs to
 * ask for parameters that are algorithm specific.
 * <p>
 * <b>Note:</b> Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @opt operations
 * @has - - - TypeInformation
 * @navassoc - - - Result
 * @depend - - - Database
 */
public interface Algorithm {
  /**
   * Runs the algorithm.
   *
   * @param database the database to run the algorithm on
   * @return the Result computed by this algorithm
   */
  Result run(Database database);

  /**
   * Get the input type restriction used for negotiating the data query.
   *
   * @return Type restriction
   */
  TypeInformation[] getInputTypeRestriction();
}
