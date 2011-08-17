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
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class MeanModel<V extends FeatureVector<V, ?>> extends BaseModel implements TextWriteable{
  /**
   * Cluster mean
   */
  private V mean;

  /**
   * Constructor with mean
   * 
   * @param mean Cluster mean
   */
  public MeanModel(V mean) {
    super();
    this.mean = mean;
  }

  /**
   * @return mean
   */
  public V getMean() {
    return mean;
  }

  /**
   * @param mean Mean vector
   */
  public void setMean(V mean) {
    this.mean = mean;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(TextWriterStream.SER_MARKER + " " + getClass().getName());
    out.commentPrintLn("Cluster Mean: " + mean.toString());
  }
}
