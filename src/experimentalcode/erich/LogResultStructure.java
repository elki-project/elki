package experimentalcode.erich;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;

/**
 * A pseudo result handler that will just show the structure of the result
 * object.
 * 
 * TODO: transform this into an evaluator, then visualize, too?
 * 
 * @author Erich Schubert
 */
public class LogResultStructure implements ResultHandler {
  Logging logger = Logging.getLogger(LogResultStructure.class);

  @Override
  public void processNewResult(@SuppressWarnings("unused") HierarchicalResult baseResult, Result newResult) {
    if(logger.isVerbose()) {
      if(newResult instanceof HierarchicalResult) {
        Hierarchy<Result> hier = ((HierarchicalResult) newResult).getHierarchy();
        if(hier != null) {
          StringBuffer buf = new StringBuffer();
          recursiveLogResult(buf, hier, newResult, 0);
          logger.verbose(buf.toString());
        }
      }
    }
  }

  /**
   * Recursively walk through the result tree.
   * 
   * @param buf Output buffer
   * @param result Current result
   * @param depth Depth
   */
  private void recursiveLogResult(StringBuffer buf, Hierarchy<Result> hier, Result result, int depth) {
    if(result == null) {
      buf.append("null");
      logger.warning("null result!");
      return;
    }
    if(depth > 50) {
      logger.warning("Probably infinitely nested results, aborting!");
      return;
    }
    for(int i = 0; i < depth; i++) {
      buf.append(" ");
    }
    buf.append(result.getClass().getSimpleName()).append(": ").append(result.getLongName());
    buf.append(" (").append(result.getShortName()).append(")\n");
    if(hier.getChildren(result).size() > 0) {
      for(Result r : hier.getChildren(result)) {
        recursiveLogResult(buf, hier, r, depth + 1);
      }
    }
  }
}
