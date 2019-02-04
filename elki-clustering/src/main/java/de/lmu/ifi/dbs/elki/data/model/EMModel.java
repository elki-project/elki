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
package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Cluster model of an EM cluster, providing a mean and a full covariance
 * Matrix.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class EMModel extends MeanModel {
  /**
   * Cluster covariance matrix
   */
  private double[][] covarianceMatrix;

  /**
   * Constructor.
   * 
   * @param mean Mean vector
   * @param covarianceMatrix Covariance matrix
   */
  public EMModel(double[] mean, double[][] covarianceMatrix) {
    super(mean);
    this.covarianceMatrix = covarianceMatrix;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Covariance Matrix: " + FormatUtil.format(covarianceMatrix, FormatUtil.NF16));
  }

  /**
   * @return covariance matrix
   */
  public double[][] getCovarianceMatrix() {
    return covarianceMatrix;
  }

  /**
   * @param covarianceMatrix covariance matrix
   */
  public void setCovarianceMatrix(double[][] covarianceMatrix) {
    this.covarianceMatrix = covarianceMatrix;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + prototypeToString() + "," + FormatUtil.format(covarianceMatrix, FormatUtil.NF8) + "]";
  }
}
