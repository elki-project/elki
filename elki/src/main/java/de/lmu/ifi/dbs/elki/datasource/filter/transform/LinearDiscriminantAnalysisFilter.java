package de.lmu.ifi.dbs.elki.datasource.filter.transform;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.inverse;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transpose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LUDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;

/**
 * Linear Discriminant Analysis (LDA) / Fisher's linear discriminant.
 * 
 * Reference:
 * <p>
 * R. A. Fisher<br />
 * The use of multiple measurements in taxonomic problems<br />
 * Annals of Eugenics 7.2 (1936): 179-188.
 * </p>
 * 
 * @author Angela Peng
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <V> Vector type
 */
@Alias("lda")
@Reference(authors = "R. A. Fisher", //
    title = "The use of multiple measurements in taxonomic problems", //
    booktitle = "Annals of eugenics 7.2 (1936)", //
    url = "http://dx.doi.org/10.1111/j.1469-1809.1936.tb02137.x")
public class LinearDiscriminantAnalysisFilter<V extends NumberVector> extends AbstractSupervisedProjectionVectorFilter<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearDiscriminantAnalysisFilter.class);

  /**
   * Constructor.
   * 
   * @param projdimension Projection dimensionality.
   */
  public LinearDiscriminantAnalysisFilter(int projdimension) {
    super(projdimension);
  }

  @Override
  protected double[][] computeProjectionMatrix(List<V> vectorcolumn, List<? extends ClassLabel> classcolumn, int dim) {
    Map<ClassLabel, TIntList> classes = partition(classcolumn);
    // Fix indexing of classes:
    List<ClassLabel> keys = new ArrayList<>(classes.keySet());
    // Compute centroids:
    List<Centroid> centroids = computeCentroids(dim, vectorcolumn, keys, classes);

    final double[][] sigmaB, sigmaI;
    // Between classes covariance:
    {
      CovarianceMatrix covmake = new CovarianceMatrix(dim);
      for(Centroid c : centroids) {
        covmake.put(c);
      }
      sigmaB = covmake.destroyToSampleMatrix();
    }
    {
      // (Average) within class variance:
      CovarianceMatrix covmake = new CovarianceMatrix(dim);
      int numc = keys.size();
      for(int i = 0; i < numc; i++) {
        double[] c = centroids.get(i).getArrayRef();
        // TODO: different weighting strategies? Sampling?
        // Note: GNU Trove iterator, not ELKI style!
        for(TIntIterator it = classes.get(keys.get(i)).iterator(); it.hasNext();) {
          covmake.put(minusEquals(vectorcolumn.get(it.next()).toArray(), c));
        }
      }
      sigmaI = covmake.destroyToSampleMatrix();
      if(new LUDecomposition(sigmaI).det() == 0) {
        for(int i = 0; i < dim; i++) {
          sigmaI[i][i] += 1e-10;
        }
      }
    }

    double[][] sol = times(inverse(sigmaI), sigmaB);
    EigenvalueDecomposition decomp = new EigenvalueDecomposition(sol);
    SortedEigenPairs sorted = new SortedEigenPairs(decomp, false);
    return transpose(sorted.eigenVectors(tdim));
  }

  /**
   * Compute the centroid for each class.
   * 
   * @param dim Dimensionality
   * @param vectorcolumn Vector column
   * @param keys Key index
   * @param classes Classes
   * @return Centroids for each class.
   */
  protected List<Centroid> computeCentroids(int dim, List<V> vectorcolumn, List<ClassLabel> keys, Map<ClassLabel, TIntList> classes) {
    final int numc = keys.size();
    List<Centroid> centroids = new ArrayList<>(numc);
    for(int i = 0; i < numc; i++) {
      Centroid c = new Centroid(dim);
      // Note: GNU Trove iterator, not ELKI style!
      for(TIntIterator it = classes.get(keys.get(i)).iterator(); it.hasNext();) {
        c.put(vectorcolumn.get(it.next()));
      }
      centroids.add(c);
    }
    return centroids;
  }

  /**
   * Class logger.
   * 
   * @return Logger
   */
  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Angela Peng
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractSupervisedProjectionVectorFilter.Parameterizer<V> {
    @Override
    protected LinearDiscriminantAnalysisFilter<V> makeInstance() {
      return new LinearDiscriminantAnalysisFilter<>(tdim);
    }
  }
}
