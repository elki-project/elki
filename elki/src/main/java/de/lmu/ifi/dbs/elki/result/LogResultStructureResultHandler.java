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
package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;

/**
 * A result handler to help with ELKI development that will just show the
 * structure of the result object.
 * 
 * This class is mostly useful when you are implementing your own result
 * handlers or evaluators, as it will print a simple representation of the
 * result tree.
 * 
 * For using this, make sure to set the logging level appropriately by using
 * {@code -verbose}.
 * 
 * TODO: transform this into an evaluator, then visualize, too?
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
@Description("Development result handler that merely logs the structure of the result tree.")
public class LogResultStructureResultHandler implements ResultHandler {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(LogResultStructureResultHandler.class);

  @Override
  public void processNewResult(Object newResult) {
    if(LOG.isVerbose()) {
      StringBuilder buf = new StringBuilder();
      recursiveLogResult(buf, newResult, 0);
      LOG.verbose(buf.toString());
    }
  }

  /**
   * Recursively walk through the result tree.
   * 
   * @param buf Output buffer
   * @param result Current result
   * @param depth Depth
   */
  private void recursiveLogResult(StringBuilder buf, Object result, int depth) {
    if(result == null) {
      buf.append("null");
      LOG.warning("null result!");
      return;
    }
    if(depth > 50) {
      LOG.warning("Probably infinitely nested results, aborting!");
      return;
    }
    for(int i = 0; i < depth; i++) {
      buf.append(' ');
    }
    buf.append(result.getClass().getSimpleName()).append(": ");
    Metadata m = Metadata.get(result);
    if(m != null) {
      buf.append(m.getLongName());
    }
    buf.append('\n');
    for(It<Object> iter = Metadata.hierarchyOf(result).iterChildren(); iter.valid(); iter.advance()) {
      recursiveLogResult(buf, iter.get(), depth + 1);
    }
  }
}
