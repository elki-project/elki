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
 * Ward's method clustering method.
 * 
 * This criterion minimizes the increase of squared errors, and should be used
 * with <em>squared Euclidean</em> distance.
 * 
 * Reference:
 * <p>
 * Ward Jr, Joe H.<br />
 * Hierarchical grouping to optimize an objective function<br />
 * Journal of the American statistical association 58.301 (1963): 236-244.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. H. Ward Jr", //
    title = "Hierarchical grouping to optimize an objective function", //
    booktitle = "Journal of the American statistical association 58.301", //
    url = "http://dx.doi.org/10.1080/01621459.1963.10500845")
@Alias({ "ward", "ssq" })
public class WardLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final WardLinkageMethod STATIC = new WardLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public WardLinkageMethod() {
    super();
  }

  @Override
  public double initial(double d, boolean issquare) {
    return .5 * (issquare ? d : (d * d));
  }

  @Override
  public double restore(double d, boolean issquare) {
    return issquare ? 2. * d : FastMath.sqrt(2. * d);
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return ((sizex + sizej) * dx + (sizey + sizej) * dy - sizej * dxy) / (double) (sizex + sizey + sizej);
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
    protected WardLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
