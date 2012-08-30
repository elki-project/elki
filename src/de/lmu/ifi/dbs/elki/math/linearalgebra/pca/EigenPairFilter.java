package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * The eigenpair filter is used to filter eigenpairs (i.e. eigenvectors
 * and their corresponding eigenvalues) which are a result of a
 * Variance Analysis Algorithm, e.g. Principal Component Analysis.
 * The eigenpairs are filtered into two types: strong and weak eigenpairs,
 * where strong eigenpairs having high variances
 * and weak eigenpairs having small variances.
 *
 * @author Elke Achtert
 * 
 * @apiviz.uses SortedEigenPairs oneway - - reads
 * @apiviz.uses FilteredEigenPairs oneway - - «create»
 */
public interface EigenPairFilter extends Parameterizable {
  /**
   * Filters the specified eigenpairs into strong and weak eigenpairs,
   * where strong eigenpairs having high variances
   * and weak eigenpairs having small variances.
   *
   * @param eigenPairs the eigenPairs (i.e. the eigenvectors and
   * @return the filtered eigenpairs
   */
  FilteredEigenPairs filter(SortedEigenPairs eigenPairs);
}