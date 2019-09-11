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
package elki.projection;

import elki.AbstractAlgorithm;
import elki.database.relation.Relation;
import elki.index.Index;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.OptionID;

/**
 * Abstract base class for projection algorithms.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <R> Result type.
 */
public abstract class AbstractProjectionAlgorithm<R> extends AbstractAlgorithm<R> {
  /**
   * Keep the original data relation.
   */
  private boolean keep;

  /**
   * Flag to keep the original projection
   */
  public static final OptionID KEEP_ID = new OptionID("tsne.retain-original", "Retain the original data.");

  /**
   * Constructor.
   *
   * @param keep Keep the original projection.
   */
  public AbstractProjectionAlgorithm(boolean keep) {
    super();
    this.keep = keep;
  }

  /**
   * Remove the previous relation.
   *
   * Manually also log index statistics, as we may be removing indexes.
   *
   * @param relation Relation to remove
   */
  protected void removePreviousRelation(Relation<?> relation) {
    if(keep) {
      return;
    }
    boolean first = true;
    for(It<Index> it = Metadata.hierarchyOf(relation).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
      if(first) {
        Logging.getLogger(getClass()).statistics("Index statistics when removing initial data relation.");
        first = false;
      }
      it.get().logStatistics();
    }
    ResultUtil.removeRecursive(relation);
  }

}
