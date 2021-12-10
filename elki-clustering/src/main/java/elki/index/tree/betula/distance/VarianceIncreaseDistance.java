/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.index.tree.betula.distance;

import elki.data.NumberVector;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Variance increase distance.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Numerically Stable CF-Trees for BIRCH Clustering<br>
 * Int. Conf on Similarity Search and Applications 2020
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 *
 * @author Andreas Lang
 * @author Erich Schubert
 */
@Alias({ "D4" })
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Numerically Stable CF-Trees for BIRCH Clustering", //
    booktitle = "Int. Conf on Similarity Search and Applications", //
    url = "https://doi.org/10.1007/978-3-030-60936-8_22", //
    bibkey = "DBLP:conf/sisap/LangS20")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS21")
public class VarianceIncreaseDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final VarianceIncreaseDistance STATIC = new VarianceIncreaseDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusterFeature cf) {
    return SquaredEuclideanDistance.STATIC.distance(cf, nv) * cf.getWeight() / (cf.getWeight() + 1);
  }

  @Override
  public double squaredDistance(ClusterFeature cf1, ClusterFeature cf2) {
    return SquaredEuclideanDistance.STATIC.distance(cf1, cf2) * cf1.getWeight() * cf2.getWeight() / (cf1.getWeight() + cf2.getWeight());
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public VarianceIncreaseDistance make() {
      return STATIC;
    }
  }
}
