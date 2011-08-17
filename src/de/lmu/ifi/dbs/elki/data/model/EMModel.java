package de.lmu.ifi.dbs.elki.data.model;
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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model of an EM cluster, providing a mean and a full covariance
 * Matrix.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class EMModel<V extends FeatureVector<V, ?>> extends MeanModel<V> {
  /**
   * Cluster covariance matrix
   */
  private Matrix covarianceMatrix;

  /**
   * Constructor.
   * 
   * @param mean Mean vector
   * @param covarianceMatrix Covariance matrix
   */
  public EMModel(V mean, Matrix covarianceMatrix) {
    super(mean);
    this.covarianceMatrix = covarianceMatrix;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Mean: " + out.normalizationRestore(this.getMean()).toString());
    out.commentPrintLn("Covariance Matrix: " + this.covarianceMatrix.toString());
  }

  /**
   * @return covariance matrix
   */
  public Matrix getCovarianceMatrix() {
    return covarianceMatrix;
  }

  /**
   * @param covarianceMatrix covariance matrix
   */
  public void setCovarianceMatrix(Matrix covarianceMatrix) {
    this.covarianceMatrix = covarianceMatrix;
  }
}
