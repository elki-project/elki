package de.lmu.ifi.dbs.elki.data.model;
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Hierarchical cluster, with prototype.
 * 
 * @author Julian Erhard
 */
public class PrototypeDendrogramModel extends DendrogramModel {
  /**
   * Prototype of this cluster
   */
  protected DBID prototype;

  /**
   * Constructor.
   *
   * @param distance Merging distance
   * @param prototype Prototype
   */
  public PrototypeDendrogramModel(double distance, DBID prototype) {
    super(distance);
    this.prototype = prototype;
  }

  /**
   * @return prototype
   */
  public DBID getPrototype() {
    return prototype;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Cluster " + getPrototypeType() + ": " + prototype.toString());
  }

  /**
   * Type of prototype (Median, Mean, ...) for printing.
   * 
   * @return String name
   */
  protected String getPrototypeType() {
    return "Prototype";
  }
}
