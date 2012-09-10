package de.lmu.ifi.dbs.elki.data.projection;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class NumericalFeatureSelection<V extends NumberVector<?>> implements Projection<V, V> {
  /**
   * Minimum dimensionality required for projection.
   */
  private int mindim;

  /**
   * Object factory.
   */
  private NumberVector.Factory<V, ?> factory;

  /**
   * Output dimensionality.
   */
  private int dimensionality;

  /**
   * Subspace.
   */
  private BitSet bits;

  /**
   * Constructor.
   * 
   * @param bits Dimensions
   * @param factory Object factory
   */
  public NumericalFeatureSelection(BitSet bits, NumberVector.Factory<V, ?> factory) {
    super();
    this.bits = bits;
    this.factory = factory;
    this.dimensionality = bits.cardinality();

    int mind = 0;
    for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
      mind = Math.max(mind, i + 1);
    }
    this.mindim = mind;
  }

  @Override
  public V project(V data) {
    double[] dbl = new double[dimensionality];
    for(int i = bits.nextSetBit(0), j = 0; i >= 0; i = bits.nextSetBit(i + 1), j++) {
      dbl[j] = data.doubleValue(i + 1);
    }
    return factory.newNumberVector(dbl);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getRestrictionClass();
    return new VectorFieldTypeInformation<V>(cls, dimensionality);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getRestrictionClass();
    return new VectorTypeInformation<V>(cls, mindim, Integer.MAX_VALUE);
  }
}