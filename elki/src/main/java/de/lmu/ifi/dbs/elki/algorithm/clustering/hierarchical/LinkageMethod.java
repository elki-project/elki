package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

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

/**
 * Abstract interface for implementing a new linkage method into hierarchical
 * clustering.
 * 
 * Reference:
 * <p>
 * G. N. Lance and W. T. Williams<br />
 * A general theory of classificatory sorting strategies 1. Hierarchical systems
 * <br/>
 * The computer journal 9.4 (1967): 373-380.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "G. N. Lance and W. T. Williams", //
title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", //
booktitle = "The computer journal 9.4", //
url = "http://dx.doi.org/ 10.1093/comjnl/9.4.373")
public interface LinkageMethod {
  /**
   * Compute combined linkage for two clusters.
   * 
   * @param sizex Size of first cluster x before merging
   * @param dx Distance of cluster x to j before merging
   * @param sizey Size of second cluster y before merging
   * @param dy Distance of cluster y to j before merging
   * @param sizej Size of candidate cluster j
   * @param dxy Distance between clusters x and y before merging
   * @return Combined distance
   */
  double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy);
}
