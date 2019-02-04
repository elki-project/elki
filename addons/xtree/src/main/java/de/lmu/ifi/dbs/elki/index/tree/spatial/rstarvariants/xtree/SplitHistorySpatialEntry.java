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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util.SplitHistory;

/**
 * Spatial entry with split history.
 *
 * @author Marisa Thoma
 * @since 0.7.5
 */
public interface SplitHistorySpatialEntry extends Entry, SpatialComparable {
  /**
   * Get the split history of this entry's node.
   * 
   * @return the split history of this entry's node
   */
  SplitHistory getSplitHistory();

  /**
   * Add a dimension to this entry's split history.
   *
   * @param dimension dimension to be added to split history
   */
  void addSplitDimension(int dimension);

  /**
   * Set the split history of this entry's node.
   *
   * @param splitHistory
   */
  void setSplitHistory(SplitHistory splitHistory);
}
