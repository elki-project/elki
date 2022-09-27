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
package elki.index.tree.betula.distance;

import elki.data.NumberVector;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Average Radius (R) criterion.
 * <p>
 * References:
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
 * @since 0.8.0
 */
@Alias("R")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Numerically Stable CF-Trees for BIRCH Clustering", //
    booktitle = "Int. Conf on Similarity Search and Applications", //
    url = "https://doi.org/10.1007/978-3-030-60936-8_22", //
    bibkey = "DBLP:conf/sisap/LangS20")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class RadiusDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final RadiusDistance STATIC = new RadiusDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusterFeature cf1) {
    return cf1.getWeight() <= 0 ? 0 : //
        (cf1.getWeight() / (cf1.getWeight() + 1.) * SquaredEuclideanDistance.STATIC.distance(cf1, nv) + cf1.sumdev()) / (cf1.getWeight() + 1.);
  }

  @Override
  public double squaredDistance(ClusterFeature cf1, ClusterFeature cf2) {
    final double n1 = cf1.getWeight(), n2 = cf2.getWeight(), n12 = n1 + n2;
    return n12 <= 0 ? 0 : //
        (n1 * n2 / n12 * SquaredEuclideanDistance.STATIC.distance(cf1, cf2) + cf1.sumdev() + cf2.sumdev()) / n12;
  }

  @Override
  public double matSelfInit(ClusterFeature cf) {
    return cf.sumdev() / cf.getWeight();
  }

  /**
   * Parameterization class
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public RadiusDistance make() {
      return STATIC;
    }
  }
}
