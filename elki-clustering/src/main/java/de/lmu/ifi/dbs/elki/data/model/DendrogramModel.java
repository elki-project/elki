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

/**
 * Model for dendrograms, provides the height of this subtree.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
public class DendrogramModel implements Model {
  /**
   * Distance to child cluster
   */
  private double height;

  /**
   * Constructor.
   * 
   * @param distance Distance to child cluster.
   */
  public DendrogramModel(double distance) {
    super();
    this.height = distance;
  }

  /**
   * @return the distance
   */
  public double getDistance() {
    return height;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + getClass().getName());
    out.commentPrintLn("Cluster height: " + height);
  }

  @Override
  public String toString() {
    return "Distance to children: " + height;
  }
}
