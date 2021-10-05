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
package elki.clustering.hierarchical.betula.distance;

import elki.clustering.hierarchical.betula.features.ClusterFeature;
import elki.data.NumberVector;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Average intracluster distance.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems (under review)
 *
 * @author Andreas Lang
 * @author Erich Schubert
 */
@Alias({ "D3" })
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems (under review)")
public class AverageIntraclusterDistance implements CFDistance {
  /**
   * Static instance.
   */
  public static final AverageIntraclusterDistance STATIC = new AverageIntraclusterDistance();

  @Override
  public double squaredDistance(NumberVector nv, ClusterFeature cf1) {
    return cf1.getWeight() <= 0 ? 0 : //
        2 * ((cf1.getWeight() + 1) * cf1.sumdev() + cf1.getWeight() * cf1.squaredCenterDistance(nv)) / ((cf1.getWeight() + 1) * cf1.getWeight());
  }

  @Override
  public double squaredDistance(ClusterFeature cf1, ClusterFeature cf2) {
    final double n12 = cf1.getWeight() + cf2.getWeight();
    return n12 <= 0 ? 0 : //
        2 * (n12 * (cf1.sumdev() + cf2.sumdev()) + cf1.getWeight() * cf2.getWeight() * cf1.squaredCenterDistance(cf2)) / (n12 * (n12 - 1));
  }

  @Override
  public double matSelfInit(ClusterFeature cf) {
    return 2 * cf.sumdev() / (cf.getWeight() - 1);
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    @Override
    public AverageIntraclusterDistance make() {
      return STATIC;
    }
  }
}
