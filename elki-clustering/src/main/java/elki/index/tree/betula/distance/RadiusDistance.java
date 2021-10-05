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
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems (under review)
 *
 * @author Andreas Lang
 * @author Erich Schubert
 */
@Alias("R")
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems (under review)")
public class RadiusDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final RadiusDistance STATIC = new RadiusDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusterFeature cf1) {
    return cf1.getWeight() <= 0 ? 0 : //
        (cf1.getWeight() / (cf1.getWeight() + 1.) * cf1.squaredCenterDistance(nv) + cf1.sumdev()) / (cf1.getWeight() + 1.);
  }

  @Override
  public double squaredDistance(ClusterFeature cf1, ClusterFeature cf2) {
    final double n1 = cf1.getWeight(), n2 = cf2.getWeight(), n12 = n1 + n2;
    return n12 <= 0 ? 0 : //
        (n1 * n2 / n12 * cf1.squaredCenterDistance(cf2) + cf1.sumdev() + cf2.sumdev()) / n12;
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
