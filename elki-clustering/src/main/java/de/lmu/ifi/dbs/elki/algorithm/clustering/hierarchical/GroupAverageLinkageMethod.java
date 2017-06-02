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

/**
 * Group-average linkage clustering method.
 * 
 * Reference:
 * <p>
 * A. K. Jain and R. C. Dubes<br />
 * Algorithms for Clustering Data<br />
 * Prentice-Hall
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Alias({ "upgma", "average", "average-link", "average-linkage", "UPGMA" })
@Reference(authors = "A. K. Jain and R. C. Dubes", //
    title = "Algorithms for Clustering Data", //
    booktitle = "Algorithms for Clustering Data, Prentice-Hall")
public class GroupAverageLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final GroupAverageLinkageMethod STATIC = new GroupAverageLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public GroupAverageLinkageMethod() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return (sizex * dx + sizey * dy) / (double) (sizex + sizey);
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
    protected GroupAverageLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
