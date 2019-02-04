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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The default R-Tree insertion strategy: find rectangle with least volume
 * enlargement.
 * <p>
 * Reference:
 * <p>
 * A. Guttman<br>
 * R-Trees: A Dynamic Index Structure For Spatial Searching<br>
 * Proc. 1984 ACM SIGMOD Int. Conf. Management of Data
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "A. Guttman", //
    title = "R-Trees: A Dynamic Index Structure For Spatial Searching", //
    booktitle = "Proc. 1984 ACM SIGMOD Int. Conf. on Management of Data", //
    url = "https://doi.org/10.1145/971697.602266", //
    bibkey = "doi:10.1145/971697.602266")
public class LeastEnlargementInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastEnlargementInsertionStrategy STATIC = new LeastEnlargementInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastEnlargementInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    double leastEnlargement = Double.POSITIVE_INFINITY;
    int best = -1;
    for(int i = 0; i < size; i++) {
      SpatialComparable entry = getter.get(options, i);
      double enlargement = SpatialUtil.enlargement(entry, obj);
      if(enlargement < leastEnlargement) {
        leastEnlargement = enlargement;
        best = i;
      }
    }
    assert (best > -1);
    return best;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LeastEnlargementInsertionStrategy makeInstance() {
      return LeastEnlargementInsertionStrategy.STATIC;
    }
  }
}