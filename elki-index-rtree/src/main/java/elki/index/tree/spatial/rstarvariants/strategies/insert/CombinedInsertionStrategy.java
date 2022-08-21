/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.spatial.rstarvariants.strategies.insert;

import elki.data.spatial.SpatialComparable;
import elki.utilities.datastructures.arraylike.ArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ClassParameter;

/**
 * Use two different insertion strategies for directory and leaf nodes.
 * <p>
 * Using two different strategies was likely first suggested in:
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
public class CombinedInsertionStrategy implements InsertionStrategy {
  /**
   * Strategy when inserting into directory nodes
   */
  InsertionStrategy dirStrategy;

  /**
   * Strategy when inserting into leaf nodes.
   */
  InsertionStrategy leafStrategy;

  /**
   * Constructor.
   * 
   * @param dirStrategy Strategy for directory nodes
   * @param leafStrategy Strategy for leaf nodes
   */
  public CombinedInsertionStrategy(InsertionStrategy dirStrategy, InsertionStrategy leafStrategy) {
    super();
    this.dirStrategy = dirStrategy;
    this.leafStrategy = leafStrategy;
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    if(depth + 1 >= height) {
      return leafStrategy.choose(options, getter, obj, height, depth);
    }
    else {
      return dirStrategy.choose(options, getter, obj, height, depth);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Insertion strategy for directory nodes.
     */
    public static final OptionID DIR_STRATEGY_ID = new OptionID("rtree.insert-directory", "Insertion strategy for directory nodes.");

    /**
     * Insertion strategy for leaf nodes.
     */
    public static final OptionID LEAF_STRATEGY_ID = new OptionID("rtree.insert-leaf", "Insertion strategy for leaf nodes.");

    /**
     * Strategy when inserting into directory nodes
     */
    InsertionStrategy dirStrategy;

    /**
     * Strategy when inserting into leaf nodes.
     */
    InsertionStrategy leafStrategy;

    @Override
    public void configure(Parameterization config) {
      new ClassParameter<InsertionStrategy>(DIR_STRATEGY_ID, InsertionStrategy.class, LeastEnlargementWithAreaInsertionStrategy.class) //
          .grab(config, x -> dirStrategy = x);
      new ClassParameter<InsertionStrategy>(LEAF_STRATEGY_ID, InsertionStrategy.class, LeastOverlapInsertionStrategy.class) //
          .grab(config, x -> leafStrategy = x);
    }

    @Override
    public CombinedInsertionStrategy make() {
      return new CombinedInsertionStrategy(dirStrategy, leafStrategy);
    }
  }
}