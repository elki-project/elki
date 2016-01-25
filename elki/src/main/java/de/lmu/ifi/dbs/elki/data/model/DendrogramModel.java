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

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for dendrograms, provides the distance to the child cluster.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
// TODO: comments
public class DendrogramModel extends AbstractModel {
  /**
   * Distance to child cluster
   */
  private double distance;

  /**
   * Constructor.
   * 
   * @param distance Distance to child cluster.
   */
  public DendrogramModel(double distance) {
    super();
    this.distance = distance;
  }

  /**
   * @return the distance
   */
  public double getDistance() {
    return distance;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Distance to children: " + distance);
  }

  @Override
  public String toString() {
    return "Distance to children: " + distance;
  }
}
