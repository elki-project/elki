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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Minimum increase in variance (MIVAR) linkage.
 * <p>
 * This is subtly different from Ward's method ({@link WardLinkage}, MISSQ),
 * because variance is normalized by the cluster size; and Ward minimizes the
 * increase in sum of squares (without normalization).
 * \[d_{\text{MIVAR}}(A\cup B,C)=
 * \left(\tfrac{|A|+|C|}{|A|+|B|+|C|}\right)^2 d(A,C) +
 * \left(\tfrac{|B|+|C|}{|A|+|B|+|C|}\right)^2 d(B,C)
 * - \tfrac{|C|\cdot(|A|+|B|)}{(|A|+|B|+|C|)^2} d(A,B)\]
 * or equivalently:
 * \[d_{\text{MIVAR}}(A\cup B,C)=\tfrac{(|A|+|C|)^2 d(A,C)
 * + (|B|+|C|)^2 d(B,C) - |C|\cdot(|A|+|B|) d(A,B)}{(|A|+|B|+|C|)^2}\]
 * <p>
 * Reference:
 * <p>
 * J. Podani<br>
 * New Combinatorial Clustering Methods<br>
 * Vegetatio 81(1/2)
 * <p>
 * attributes this to (but we did not verify):
 * <p>
 * E. Diday, J. Lemaire, J. Pouget, F. Testu<br>
 * Elements d'analyse de donnees
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "J. Podani", //
    title = "New Combinatorial Clustering Methods", //
    booktitle = "Vegetatio 81(1/2)", //
    url = "https://doi.org/10.1007/978-94-009-2432-1_5", //
    bibkey = "doi:10.1007/978-94-009-2432-1_5")
@Reference(authors = "E. Diday, J. Lemaire, J. Pouget, F. Testu", //
    title = "Elements d'analyse de donnees", booktitle = "", //
    bibkey = "books/misc/DidayLPT85")
@Alias({ "variance", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.MinimumVarianceLinkageMethod" })
public class MinimumVarianceLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final MinimumVarianceLinkage STATIC = new MinimumVarianceLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public MinimumVarianceLinkage() {
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
    final int xj = sizex + sizej, yj = sizey + sizej;
    final int n = sizex + sizey + sizej;
    return (xj * (double) xj * dx + yj * (double) yj * dy - (sizej * (double) (sizex + sizey)) * dxy) / (n * (double) n);
  }

  /**
   * Class parameterizer.
   * <p>
   * Returns the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MinimumVarianceLinkage makeInstance() {
      return STATIC;
    }
  }
}
