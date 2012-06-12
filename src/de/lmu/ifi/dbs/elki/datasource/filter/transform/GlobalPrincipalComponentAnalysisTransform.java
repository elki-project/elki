package de.lmu.ifi.dbs.elki.datasource.filter.transform;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Apply principal component analysis to the data set.
 * 
 * TODO: add dimensionality reduction
 * 
 * @author Erich Schubert
 * 
 * @param <O> Vector type
 */
public class GlobalPrincipalComponentAnalysisTransform<O extends NumberVector<O, ?>> extends AbstractConversionFilter<O, O> {
  int dim = -1;

  CovarianceMatrix covmat = null;

  double[][] proj = null;

  double[] buf = null;

  double[] mean = null;

  /**
   * Constructor.
   */
  public GlobalPrincipalComponentAnalysisTransform() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<O> in) {
    if(!(in instanceof VectorFieldTypeInformation)) {
      throw new AbortException("PCA can only applied to fixed dimensionality vectors");
    }
    dim = ((VectorFieldTypeInformation<?>) in).dimensionality();
    covmat = new CovarianceMatrix(dim);
    return true;
  }

  @Override
  protected void prepareProcessInstance(O obj) {
    covmat.put(obj);
  }

  @Override
  protected void prepareComplete() {
    mean  = covmat.getMeanVector().getArrayRef();
    PCAResult pcares = (new PCARunner<O>(null)).processCovarMatrix(covmat.destroyToSampleMatrix());
    SortedEigenPairs eps = pcares.getEigenPairs();
    covmat = null;

    proj = new double[dim][dim];
    for(int d = 0; d < dim; d++) {
      EigenPair ep = eps.getEigenPair(d);
      double[] ev = ep.getEigenvector().getArrayRef();
      double eval = Math.sqrt(ep.getEigenvalue());
      // Fill weighted and transposed:
      for(int i = 0; i < dim; i++) {
        proj[d][i] = ev[i] / eval;
      }
    }
    buf = new double[dim];
  }

  @Override
  protected O filterSingleObject(O obj) {
    // Shift by mean and copy
    for(int i = 0; i < dim; i++) {
      buf[i] = obj.doubleValue(i + 1) - mean[i];
    }
    double[] p = VMath.times(proj, buf);
    return obj.newNumberVector(p);
  }

  @Override
  protected SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<O> in) {
    return in;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractParameterizer {
    @Override
    protected Object makeInstance() {
      return new GlobalPrincipalComponentAnalysisTransform<O>();
    }
  }
}