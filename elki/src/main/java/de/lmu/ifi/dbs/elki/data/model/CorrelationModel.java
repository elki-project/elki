package de.lmu.ifi.dbs.elki.data.model;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Cluster model using a filtered PCA result and an centroid.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.composedOf PCAFilteredResult
 */
public class CorrelationModel extends SimplePrototypeModel<double[]> {
  /**
   * The computed PCA result of this cluster.
   */
  private PCAFilteredResult pcaresult;

  /**
   * Constructor
   * 
   * @param pcaresult PCA result
   * @param centroid Centroid
   */
  public CorrelationModel(PCAFilteredResult pcaresult, double[] centroid) {
    super(centroid);
    this.pcaresult = pcaresult;
  }

  /**
   * Get assigned PCA result
   * 
   * @return PCA result
   */
  public PCAFilteredResult getPCAResult() {
    return pcaresult;
  }

  /**
   * Assign new PCA result
   * 
   * @param pcaresult PCA result
   */
  public void setPCAResult(PCAFilteredResult pcaresult) {
    this.pcaresult = pcaresult;
  }

  /**
   * Implementation of {@link TextWriteable} interface
   * 
   * @param label unused parameter
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + getClass().getName());
    out.commentPrintLn("Centroid: " + out.normalizationRestore(getPrototype()).toString());
    out.commentPrintLn("Strong Eigenvectors:");
    String strong = getPCAResult().getStrongEigenvectors().toString();
    while(strong.endsWith("\n")) {
      strong = strong.substring(0, strong.length() - 1);
    }
    out.commentPrintLn(strong);
    out.commentPrintLn("Weak Eigenvectors:");
    String weak = getPCAResult().getWeakEigenvectors().toString();
    while(weak.endsWith("\n")) {
      weak = weak.substring(0, weak.length() - 1);
    }
    out.commentPrintLn(weak);
    out.commentPrintLn("Eigenvalues: " + FormatUtil.format(getPCAResult().getEigenvalues(), " ", FormatUtil.NF2));
  }
}
