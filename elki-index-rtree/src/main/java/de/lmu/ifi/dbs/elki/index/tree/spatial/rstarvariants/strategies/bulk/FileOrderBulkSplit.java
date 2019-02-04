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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Trivial bulk loading - assumes that the file has been appropriately sorted
 * before.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class FileOrderBulkSplit extends AbstractBulkSplit {
  /**
   * Static instance
   */
  public static final FileOrderBulkSplit STATIC = new FileOrderBulkSplit();

  /**
   * Constructor.
   */
  protected FileOrderBulkSplit() {
    super();
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries) {
    return trivialPartition(spatialObjects, minEntries, maxEntries);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected FileOrderBulkSplit makeInstance() {
      return FileOrderBulkSplit.STATIC;
    }
  }
}