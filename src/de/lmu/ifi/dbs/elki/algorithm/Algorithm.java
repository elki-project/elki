package de.lmu.ifi.dbs.elki.algorithm;

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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * <p>
 * Specifies the requirements for any algorithm that is to be executable by the
 * main class.
 * </p>
 * <p/>
 * <p>
 * Any implementation needs not to take care of input nor output, parsing and so
 * on. Those tasks are performed by the framework. An algorithm simply needs to
 * ask for parameters that are algorithm specific.
 * </p>
 * <p/>
 * <p>
 * <b>Note:</b> Any implementation is supposed to provide a constructor without
 * parameters (default constructor).
 * </p>
 * 
 * @author Arthur Zimek
 */
public interface Algorithm extends Parameterizable {
  /**
   * Runs the algorithm.
   * 
   * @param database the database to run the algorithm on
   * @return the Result computed by this algorithm
   * @throws IllegalStateException if the algorithm has not been initialized
   *         properly (e.g. the setParameters(String[]) method has been failed
   *         to be called).
   */
  Result run(Database database) throws IllegalStateException;

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  public TypeInformation[] getInputTypeRestriction();
}