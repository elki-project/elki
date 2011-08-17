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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Null Algorithm, which does nothing. Can be used to e.g. just visualize a data
 * set.
 * 
 * @author Erich Schubert
 */
@Title("Null Algorithm")
@Description("Algorithm which does nothing, just return a null object.")
public class NullAlgorithm extends AbstractAlgorithm<Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(NullAlgorithm.class);
  
  /**
   * Constructor.
   */
  public NullAlgorithm() {
    super();
  }

  @Override
  public Result run(@SuppressWarnings("unused") Database database) throws IllegalStateException {
    return null;
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array();
  }
}