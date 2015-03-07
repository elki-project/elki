package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util.SplitHistory;

public interface SplitHistorySpatialEntry extends Entry, SpatialComparable {
  /**
   * Get the split history of this entry's node.
   * 
   * @return the split history of this entry's node
   */
  public SplitHistory getSplitHistory();

  /**
   * Add a dimension to this entry's split history.
   * 
   * @param dimension dimension to be added to split history
   */
  public void addSplitDimension(int dimension);

  /**
   * Set the split history of this entry's node.
   * 
   * @param splitHistory
   */
  public void setSplitHistory(SplitHistory splitHistory);
}
