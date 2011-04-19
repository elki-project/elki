package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Normalization performs a normalization on a set of feature vectors and is
 * capable to transform a set of feature vectors to the original attribute
 * ranges. <p/> It can also transform a matrix describing an equation system of
 * linear dependencies derived on the normalized space to describe linear
 * dependencies quantitatively adapted to the original space.
 *
 * @author Arthur Zimek
 * @param <O> object type
 */
public interface Normalization<O> extends ObjectFilter, Parameterizable {
  /**
   * Performs a normalization on a list of database objects and their associations.
   *
   * @param objectAndAssociationsList the list of database objects and their associations
   * @return a list of normalized database objects and their associations corresponding
   *         to the given list
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  MultipleObjectsBundle normalizeObjects(MultipleObjectsBundle objects) throws NonNumericFeaturesException;

  /**
   * Performs a normalization on a set of feature vectors.
   *
   * @param featureVectors a set of feature vectors to be normalized
   * @return a set of normalized feature vectors corresponding to the given
   *         feature vectors but being different objects
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  @Deprecated
  List<O> normalize(List<O> featureVectors) throws NonNumericFeaturesException;

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