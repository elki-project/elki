/**
 * Parallelized variants of LOF.
 * 
 * This parallelization is based on the generalization of outlier detection published in:
 * 
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br />
 * Local Outlier Detection Reconsidered: a Generalized View on Locality with
 * Applications to Spatial, Video, and Network Outlier Detection<br />
 * Data Mining and Knowledge Discovery, 28(1): 190–237, 2014.
 * </p>
 */
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

@Reference(authors = "E. Schubert, A. Zimek, H.-P. Kriegel", //
title = "Local Outlier Detection Reconsidered: a Generalized View on Locality with Applications to Spatial, Video, and Network Outlier Detection", //
booktitle = "Data Mining and Knowledge Discovery, 28(1): 190–237, 2014.", //
url = "http://dx.doi.org/10.1007/s10618-012-0300-z")
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof.parallel;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

