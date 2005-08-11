package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.util.List;

/**
 * Normalization performs a normalization on a set of
 * feature vectors and is capable to transform a set of
 * feature vectores to the original attribute ranges.
 * 
 * It can also transform a matrix describing an equation system
 * of linear dependencies derived on the normalized space to describe
 * linear dependencies quantitatively adapted to the original
 * space.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Normalization
{
    /**
     * Performs a normalization on a set of
     * feature vectors.
     * 
     * 
     * @param featureVectors a set of feature vectors to be normalized
     * @return a set of normalized feature vectors corresponding to the
     * given feature vectors
     */
    List<FeatureVector> normalize(List<FeatureVector> featureVectors);
    
    /**
     * Transforms a set of
     * feature vectores to the original attribute ranges.
     * 
     * 
     * @param featureVectors a set of feature vectors to be transformed into
     * original space
     * @return a set of feature vectors transformed into original space
     * corresponding to the given feature vectors
     */
    List<FeatureVector> restore(List<FeatureVector> featureVectors);
    
    /**
     * Transforms a matrix describing an equation system
     * of linear dependencies derived on the normalized space to describe
     * linear dependencies quantitatively adapted to the original
     * space.
     * 
     * 
     * @param matrix the matrix to be transformed
     * @return a matrix describing an equation system
     * of linear dependencies derived on the normalized space transformed to describe
     * linear dependencies quantitatively adapted to the original space
     */
    Matrix transform(Matrix matrix);
    
}
