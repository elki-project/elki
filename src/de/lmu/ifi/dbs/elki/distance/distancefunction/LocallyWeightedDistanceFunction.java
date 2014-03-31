package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a locally weighted distance function. Computes the quadratic form
 * distance between two vectors P and Q as follows:
 * 
 * result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)} where
 * dist<sub>X</sub>(X,Y) = (X-Y)*<b>M<sub>X</sub></b>*(X-Y)<b><sup>T</sup></b>
 * and <b>M<sub>X</sub></b> is the weight matrix of vector X.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class LocallyWeightedDistanceFunction<V extends NumberVector> extends AbstractIndexBasedDistanceFunction<V, FilteredLocalPCAIndex<V>> implements FilteredLocalPCABasedDistanceFunction<V, FilteredLocalPCAIndex<V>> {
  /**
   * Constructor
   * 
   * @param indexFactory Index factory
   */
  public LocallyWeightedDistanceFunction(LocalProjectionIndex.Factory<V, FilteredLocalPCAIndex<V>> indexFactory) {
    super(indexFactory);
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public <T extends V> Instance<T> instantiate(Relation<T> database) {
    // We can't really avoid these warnings, due to a limitation in Java
    // Generics (AFAICT)
    @SuppressWarnings("unchecked")
    LocalProjectionIndex<T, ?> indexinst = (LocalProjectionIndex<T, ?>) indexFactory.instantiate((Relation<V>) database);
    return new Instance<>(database, indexinst, this);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    if(this.indexFactory.equals(((LocallyWeightedDistanceFunction<?>) obj).indexFactory)) {
      return false;
    }
    return true;
  }

  /**
   * Instance of this distance for a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector> extends AbstractIndexBasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, LocallyWeightedDistanceFunction<? super V>> implements FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index
     * @param distanceFunction Distance Function
     */
    public Instance(Relation<V> database, LocalProjectionIndex<V, ?> index, LocallyWeightedDistanceFunction<? super V> distanceFunction) {
      super(database, index, distanceFunction);
    }

    /**
     * Computes the distance between two given real vectors according to this
     * distance function.
     * 
     * @param id1 first object id
     * @param id2 second object id
     * @return the distance between two given real vectors according to this
     *         distance function
     */
    @Override
    public double distance(DBIDRef id1, DBIDRef id2) {
      Matrix m1 = index.getLocalProjection(id1).similarityMatrix(), m2 = index.getLocalProjection(id2).similarityMatrix();

      if(m1 == null || m2 == null) {
        return Double.POSITIVE_INFINITY;
      }

      V v1 = relation.get(id1), v2 = relation.get(id2);
      Vector diff = v1.getColumnVector().minusEquals(v2.getColumnVector());

      double dist1 = diff.transposeTimesTimes(m1, diff);
      double dist2 = diff.transposeTimesTimes(m2, diff);

      if(dist1 < 0) {
        if(dist1 > -1e-12) {
          dist1 = 0;
        }
        else {
          throw new IllegalArgumentException("dist1 " + dist1 + "  < 0!");
        }
      }
      if(dist2 < 0) {
        if(dist2 > -1e-12) {
          dist2 = 0;
        }
        else {
          throw new IllegalArgumentException("dist2 " + dist2 + "  < 0!");
        }
      }

      return Math.sqrt(Math.max(dist1, dist2));
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractIndexBasedDistanceFunction.Parameterizer<LocalProjectionIndex.Factory<V, FilteredLocalPCAIndex<V>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, LocalProjectionIndex.Factory.class, KNNQueryFilteredPCAIndex.Factory.class);
    }

    @Override
    protected LocallyWeightedDistanceFunction<V> makeInstance() {
      return new LocallyWeightedDistanceFunction<>(factory);
    }
  }
}