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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Hierarchical cluster, with prototype.
 * 
 * @author Julian Erhard
 * @since 0.7.5
 */
public class PrototypeDendrogramModel extends DendrogramModel implements PrototypeModel<DBID> {
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

  @Override
  public String getPrototypeType() {
    return "Prototype";
  }
}
