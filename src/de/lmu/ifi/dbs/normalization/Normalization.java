package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.util.List;

/**
 * Normalization performs a normalization on a set of feature vectors and is
 * capable to transform a set of feature vectors to the original attribute
 * ranges. <p/> It can also transform a matrix describing an equation system of
 * linear dependencies derived on the normalized space to describe linear
 * dependencies quantitatively adapted to the original space.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Normalization<O extends DatabaseObject> extends Parameterizable {
  /**
   * Performs a normalization on a set of feature vectors.
   *
   * @param featureVectors a set of feature vectors to be normalized
   * @return a set of normalized feature vectors corresponding to the given
   *         feature vectors
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
   * Transforms a matrix describing an equation system of linear dependencies
   * derived on the normalized space to describe linear dependencies
   * quantitatively adapted to the original space.
   *
   * @param matrix the matrix to be transformed
   * @return a matrix describing an equation system of linear dependencies
   *         derived on the normalized space transformed to describe linear
   *         dependencies quantitatively adapted to the original space
   * @throws NonNumericFeaturesException if specified Matrix is not compatible with values initialized
   *                                     during normalization
   */
  Matrix transform(Matrix matrix) throws NonNumericFeaturesException;

  /**
   * Returns a string representation of this normalization. The specified prefix pre will be
   * the prefix of each new line. This method is used to write the parameters of
   * a normalization to a result of an algorithm using this normalization.
   * @param pre the prefix of each new line
   * @return a string representation of this normalization
   */
  String toString(String pre);

}
