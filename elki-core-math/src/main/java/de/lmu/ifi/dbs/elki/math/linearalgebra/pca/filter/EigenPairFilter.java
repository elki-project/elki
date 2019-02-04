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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * The eigenpair filter is used to filter eigenpairs (i.e. eigenvectors and
 * their corresponding eigenvalues) which are a result of a Variance Analysis
 * Algorithm, e.g. Principal Component Analysis. The eigenpairs are filtered
 * into two types: strong and weak eigenpairs, where strong eigenpairs having
 * high variances and weak eigenpairs having small variances.
 *
 * @author Elke Achtert
 * @since 0.1
 */
public interface EigenPairFilter {
  /**
   * Parameter to specify the filter for determination of the strong and weak
   * eigenvectors, must be a subclass of {@link EigenPairFilter}.
   */
  OptionID PCA_EIGENPAIR_FILTER = new OptionID("pca.filter", "Filter class to determine the strong and weak eigenvectors.");

  /**
   * Filters the specified eigenvalues into strong and weak eigenvalues, where
   * strong eigenvalues have high variance and weak eigenvalues have small
   * variance.
   *
   * @param eigenValues the array of eigenvalues, must be sorted descending
   * @return the number of eigenvectors to keep
   */
  int filter(double[] eigenValues);
}