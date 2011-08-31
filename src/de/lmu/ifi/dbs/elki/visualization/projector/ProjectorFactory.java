package de.lmu.ifi.dbs.elki.visualization.projector;
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

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultProcessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * A projector is responsible for adding projections to the visualization by
 * detecting appropriate relations in the database.
 * 
 * @author Erich Schubert
 */
public interface ProjectorFactory extends ResultProcessor, Parameterizable {
  /**
   * Add projections for the given result (tree) to the result tree.
   * 
   * @param baseResult Context to work with
   * @param newResult Result to process
   */
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult);
}