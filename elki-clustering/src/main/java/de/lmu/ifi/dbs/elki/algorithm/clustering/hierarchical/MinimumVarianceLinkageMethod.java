/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Minimum variance linkage.
 *
 * This is subtly different from Ward's method ({@link WardLinkageMethod}),
 * because variance is normalized by the cluster size; and Ward minimizes the
 * increase in sum of squares (without normalization).
 *
 * Reference:
 * <p>
 * J. Podani<br />
 * New Combinatorial Clustering Methods<br />
 * Vegetatio 81(1/2)
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Podani", //
    title = "New Combinatorial Clustering Methods", //
    booktitle = "Vegetatio 81(1/2)", //
    url = "http://dx.doi.org/10.1007/978-94-009-2432-1_5")
@Alias({ "variance" })
public class MinimumVarianceLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final MinimumVarianceLinkageMethod STATIC = new MinimumVarianceLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public MinimumVarianceLinkageMethod() {
    super();
  }

  @Override
  public double initial(double d, boolean issquare) {
    return .25 * (issquare ? d : (d * d));
  }

  @Override
  public double restore(double d, boolean issquare) {
    return issquare ? 4. * d : FastMath.sqrt(4. * d);
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    final int xj = sizex + sizej;
    final int yj = sizey + sizej;
    final int n = sizex + sizey + sizej;
    return (xj * (double) xj * dx + yj * (double) yj * dy - (sizej * (double) (sizex + sizey)) * dxy) / (n * (double) n);
  }

  /**
   * Class parameterizer.
   * 
   * Returns the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MinimumVarianceLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
