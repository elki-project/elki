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
 * A slight modification of the default R-Tree insertion strategy: find
 * rectangle with least volume enlargement, but choose least area on ties.
 * <p>
 * Proposed for non-leaf entries in:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
public class LeastEnlargementWithAreaInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastEnlargementWithAreaInsertionStrategy STATIC = new LeastEnlargementWithAreaInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastEnlargementWithAreaInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    // As in R-Tree, with a slight modification for ties
    double leastEnlargement = Double.POSITIVE_INFINITY;
    double minArea = -1;
    int best = -1;
    for(int i = 0; i < size; i++) {
      SpatialComparable entry = getter.get(options, i);
      double enlargement = SpatialUtil.enlargement(entry, obj);
      if(enlargement < leastEnlargement) {
        leastEnlargement = enlargement;
        best = i;
        minArea = SpatialUtil.volume(entry);
      }
      else if(enlargement == leastEnlargement) {
        final double area = SpatialUtil.volume(entry);
        if(area < minArea) {
          // Tie handling proposed by R*:
          best = i;
          minArea = area;
        }
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
    protected LeastEnlargementWithAreaInsertionStrategy makeInstance() {
      return LeastEnlargementWithAreaInsertionStrategy.STATIC;
    }
  }
}