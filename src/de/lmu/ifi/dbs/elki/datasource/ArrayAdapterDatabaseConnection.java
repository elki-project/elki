package de.lmu.ifi.dbs.elki.datasource;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * Import an existing data matrix (<code>double[rows][cols]</code>) into an ELKI
 * database.
 * 
 * Note: this class is not parameterizable, but can only be used from Java.
 * 
 * @author Erich Schubert
 */
public class ArrayAdapterDatabaseConnection implements DatabaseConnection {
  /**
   * The actual data.
   */
  double[][] data;

  /**
   * Constructor.
   * 
   * @param data Existing data matrix
   */
  public ArrayAdapterDatabaseConnection(double[][] data) {
    super();
  }

  @Override
  public MultipleObjectsBundle loadData() {
    MultipleObjectsBundle b = new MultipleObjectsBundle();
    int mind = Integer.MAX_VALUE;
    int maxd = 0;
    List<DoubleVector> vecs = new ArrayList<DoubleVector>(data.length);
    for(int i = 0; i < data.length; i++) {
      mind = Math.min(mind, data[i].length);
      maxd = Math.max(maxd, data[i].length);
      vecs.add(new DoubleVector(data[i]));
    }
    SimpleTypeInformation<DoubleVector> type;
    if(mind == maxd) {
      type = new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class, mind, DoubleVector.STATIC);
    }
    else {
      type = new SimpleTypeInformation<DoubleVector>(DoubleVector.class);
    }
    b.appendColumn(type, vecs);
    return b;
  }
}