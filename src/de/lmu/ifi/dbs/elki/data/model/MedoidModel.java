package de.lmu.ifi.dbs.elki.data.model;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 */
public class MedoidModel extends BaseModel implements TextWriteable {
  /**
   * Cluster medoid
   */
  private DBID medoid;

  /**
   * Constructor with medoid
   * 
   * @param medoid Cluster medoid
   */
  public MedoidModel(DBID medoid) {
    super();
    this.medoid = medoid;
  }

  /**
   * @return medoid
   */
  public DBID getMedoid() {
    return medoid;
  }

  /**
   * @param medoid Medoid object
   */
  public void setMedoid(DBID medoid) {
    this.medoid = medoid;
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
    out.commentPrintLn("Cluster Medoid: " + medoid.toString());
  }
}
