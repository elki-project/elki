package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Single-linkage clustering method.
 * 
 * Reference:
 * <p>
 * K. Florek and J. Łukaszewicz and J. Perkal and H. Steinhaus and S. Zubrzycki<br/>
 * Sur la liaison et la division des points d'un ensemble fini<br />
 * In Colloquium Mathematicae (Vol. 2, No. 3-4)
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "K. Florek and J. Łukaszewicz and J. Perkal and H. Steinhaus and S. Zubrzycki", //
title = "Sur la liaison et la division des points d'un ensemble fini",//
booktitle = "Colloquium Mathematicae (Vol. 2, No. 3-4)")
@Alias({ "single-link", "single", "slink", "nearest", "nearest-neighbor" })
public class SingleLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final SingleLinkageMethod STATIC = new SingleLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public SingleLinkageMethod() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return dx < dy ? dx : dy;
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
    protected SingleLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
