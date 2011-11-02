package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.SubsetArrayAdapter;

/**
 * Abstract feature selection projection
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <F> Feature type
 */
public abstract class AbstractFeatureSelection<V extends FeatureVector<V, F>, F> implements Projection<V, V> {
  /**
   * Array adapter
   */
  protected SubsetArrayAdapter<F, V> adapter;

  /**
   * Constructor.
   *
   * @param adapter Data adapter
   */
  public AbstractFeatureSelection(SubsetArrayAdapter<F, V> adapter) {
    super();
    this.adapter = adapter;
  }

  @Override
  public V project(V data) {
    return data.newFeatureVector(data, adapter);
  }

  @Override
  abstract public SimpleTypeInformation<V> getOutputDataTypeInformation();

  @Override
  abstract public TypeInformation getInputDataTypeInformation();
}