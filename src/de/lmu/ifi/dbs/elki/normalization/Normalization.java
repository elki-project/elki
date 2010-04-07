package de.lmu.ifi.dbs.elki.normalization;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
public interface Normalization<O extends DatabaseObject> extends Parameterizable {
  /**
   * Performs a normalization on a list of database objects and their associations.
   *
   * @param objectAndAssociationsList the list of database objects and their associations
   * @return a list of normalized database objects and their associations corresponding
   *         to the given list
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  List<Pair<O, DatabaseObjectMetadata>> normalizeObjects(List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList) throws NonNumericFeaturesException;

  /**
   * Performs a normalization on a set of feature vectors.
   *
   * @param featureVectors a set of feature vectors to be normalized
   * @return a set of normalized feature vectors corresponding to the given
   *         feature vectors but being different objects
   * @throws NonNumericFeaturesException if feature vectors differ in length or values are not
   *                                     suitable to normalization
   */
  List<O> normalize(List<O> featureVectors) throws NonNumericFeaturesException;

  /**
   * Transforms a set of feature vectores to the original attribute ranges.
   *
   * @param featureVectors a set of feature vectors to be transformed into original space
   * @return a set of feature vectors transformed into original space
   *         corresponding to the given feature vectors
   * @throws NonNumericFeaturesException if feature vectors differ in length or are not compatible
   *                                     with values initialized during normalization
   */
  List<O> restore(List<O> featureVectors) throws NonNumericFeaturesException;

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

  /**
   * Returns a string representation of this normalization. The specified prefix pre will be
   * the prefix of each new line. This method is used to write the parameters of
   * a normalization to a result of an algorithm using this normalization.
   *
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  String toString(String pre);
}