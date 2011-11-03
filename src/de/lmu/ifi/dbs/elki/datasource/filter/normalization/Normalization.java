package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Normalization performs a normalization on a set of feature vectors and is
 * capable to transform a set of feature vectors to the original attribute
 * ranges. <p/> It can also transform a matrix describing an equation system of
 * linear dependencies derived on the normalized space to describe linear
 * dependencies quantitatively adapted to the original space.
 *
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.has NonNumericFeaturesException
 * 
 * @param <O> object type
 */
public interface Normalization<O> extends ObjectFilter, Parameterizable {
  /**
   * Performs a normalization on a database object bundle.
   *
   * @param objects the database objects package
   * @return modified object bundle
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  MultipleObjectsBundle normalizeObjects(MultipleObjectsBundle objects) throws NonNumericFeaturesException;

  /**
   * Transforms a feature vector to the original attribute ranges.
   *
   * @param featureVector a feature vector to be transformed into original space
   * @return a feature vector transformed into original space corresponding to
   *         the given feature vector
   * @throws NonNumericFeaturesException feature vector is not compatible with values initialized
   *                                     during normalization
   */
  O restore(O featureVector) throws NonNumericFeaturesException;

  /**
   * Transforms a linear equation system describing linear dependencies
   * derived on the normalized space into a linear equation system describing
   * linear dependencies quantitatively adapted to the original space.
   *
   * @param linearEquationSystem the linear equation system to be transformed
   * @return a linear equation system describing linear dependencies
   *         derived on the normalized space transformed into a linear equation system
   *         describing linear dependencies quantitatively adapted to the original space
   * @throws NonNumericFeaturesException if specified linear equation system is not compatible
   *                                     with values initialized during normalization
   */
  LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) throws NonNumericFeaturesException;
}