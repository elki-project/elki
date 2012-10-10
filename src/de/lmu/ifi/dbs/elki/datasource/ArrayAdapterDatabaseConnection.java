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
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

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
   * Object labels.
   */
  String[] labels;

  /**
   * Starting ID for fixed object ids.
   */
  Integer startid = null;

  /**
   * Constructor.
   * 
   * @param data Existing data matrix
   */
  public ArrayAdapterDatabaseConnection(double[][] data) {
    this(data, null, null);
  }

  /**
   * Constructor.
   * 
   * @param data Existing data matrix
   * @param labels Object labels
   */
  public ArrayAdapterDatabaseConnection(double[][] data, String[] labels) {
    this(data, labels, null);
  }

  /**
   * Constructor.
   * 
   * @param data Existing data matrix
   * @param labels Object labels
   * @param startid Starting object ID
   */
  public ArrayAdapterDatabaseConnection(double[][] data, String[] labels, Integer startid) {
    super();
    this.data = data;
    this.labels = labels;
    this.startid = startid;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    MultipleObjectsBundle b = new MultipleObjectsBundle();
    if(startid != null) {
      List<DBID> ids = new ArrayList<DBID>(data.length);
      for(int i = 0; i < data.length; i++) {
        ids.add(DBIDUtil.importInteger(startid.intValue() + i));
      }
      b.appendColumn(TypeUtil.DBID, Arrays.asList(labels));
    }

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
      type = new VectorFieldTypeInformation<DoubleVector>(DoubleVector.FACTORY, mind);
    }
    else {
      type = new SimpleTypeInformation<DoubleVector>(DoubleVector.class);
    }
    b.appendColumn(type, vecs);
    if(labels != null) {
      if(labels.length != data.length) {
        throw new AbortException("Label and DBID columns must have the same size.");
      }
      b.appendColumn(TypeUtil.STRING, Arrays.asList(labels));
    }
    return b;
  }
}