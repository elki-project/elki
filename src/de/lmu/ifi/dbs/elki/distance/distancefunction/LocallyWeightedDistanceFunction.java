package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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
// FIXME: implements SpatialPrimitiveDistanceFunction<V, DoubleDistance>
public class LocallyWeightedDistanceFunction<V extends NumberVector<?, ?>> extends AbstractIndexBasedDistanceFunction<V, FilteredLocalPCAIndex<V>, DoubleDistance> implements FilteredLocalPCABasedDistanceFunction<V, FilteredLocalPCAIndex<V>, DoubleDistance> {
  /**
   * Constructor
   * 
   * @param indexFactory Index factory
   */
  public LocallyWeightedDistanceFunction(LocalProjectionIndex.Factory<V, FilteredLocalPCAIndex<V>> indexFactory) {
    super(indexFactory);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
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
    return new Instance<T>(database, indexinst, this);
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
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractIndexBasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, DoubleDistance, LocallyWeightedDistanceFunction<? super V>> implements FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, DoubleDistance> {
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
    public DoubleDistance distance(DBID id1, DBID id2) {
      Matrix m1 = index.getLocalProjection(id1).similarityMatrix();
      Matrix m2 = index.getLocalProjection(id2).similarityMatrix();

      if(m1 == null || m2 == null) {
        return new DoubleDistance(Double.POSITIVE_INFINITY);
      }

      V v1 = relation.get(id1);
      V v2 = relation.get(id2);
      Vector v1Mv2 = v1.getColumnVector().minus(v2.getColumnVector());
      Vector v2Mv1 = v2.getColumnVector().minus(v1.getColumnVector());

      double dist1 = v1Mv2.transposeTimes(m1).times(v1Mv2).get(0, 0);
      double dist2 = v2Mv1.transposeTimes(m2).times(v2Mv1).get(0, 0);

      if(dist1 < 0) {
        if(-dist1 < 0.000000000001) {
          dist1 = 0;
        }
        else {
          throw new IllegalArgumentException("dist1 " + dist1 + "  < 0!");
        }
      }
      if(dist2 < 0) {
        if(-dist2 < 0.000000000001) {
          dist2 = 0;
        }
        else {
          throw new IllegalArgumentException("dist2 " + dist2 + "  < 0!");
        }
      }

      return new DoubleDistance(Math.max(Math.sqrt(dist1), Math.sqrt(dist2)));
    }

    // @Override
    // TODO: re-enable spatial interfaces
    public DoubleDistance minDistBROKEN(SpatialComparable mbr, V v) {
      if(mbr.getDimensionality() != v.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr.toString() + "\n  second argument: " + v.toString());
      }

      double[] r = new double[v.getDimensionality()];
      for(int d = 1; d <= v.getDimensionality(); d++) {
        double value = v.doubleValue(d);
        if(value < mbr.getMin(d)) {
          r[d - 1] = mbr.getMin(d);
        }
        else if(value > mbr.getMax(d)) {
          r[d - 1] = mbr.getMax(d);
        }
        else {
          r[d - 1] = value;
        }
      }

      Matrix m = null; // index.getLocalProjection(v.getID()).similarityMatrix();
      Vector rv1Mrv2 = v.getColumnVector().minus(new Vector(r));
      double dist = rv1Mrv2.transposeTimes(m).times(rv1Mrv2).get(0, 0);

      return new DoubleDistance(Math.sqrt(dist));
    }

    // TODO: Remove?
    // @Override
    // public DoubleDistance minDist(SpatialComparable mbr, DBID id) {
    // return minDist(mbr, database.get(id));
    // }

    // @Override
    // TODO: re-enable spatial interface
    public DoubleDistance distance(SpatialComparable mbr1, SpatialComparable mbr2) {
      if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n  first argument: " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
      }

      double sqrDist = 0;
      for(int d = 1; d <= mbr1.getDimensionality(); d++) {
        double m1, m2;
        if(mbr1.getMax(d) < mbr2.getMin(d)) {
          m1 = mbr1.getMax(d);
          m2 = mbr2.getMin(d);
        }
        else if(mbr1.getMin(d) > mbr2.getMax(d)) {
          m1 = mbr1.getMin(d);
          m2 = mbr2.getMax(d);
        }
        else { // The mbrs intersect!
          m1 = 0;
          m2 = 0;
        }
        double manhattanI = m1 - m2;
        sqrDist += manhattanI * manhattanI;
      }
      return new DoubleDistance(Math.sqrt(sqrDist));
    }

    // @Override
    // TODO: re-enable spatial interface
    public DoubleDistance centerDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
      if(mbr1.getDimensionality() != mbr2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of objects\n first argument:  " + mbr1.toString() + "\n  second argument: " + mbr2.toString());
      }

      double sqrDist = 0;
      for(int d = 1; d <= mbr1.getDimensionality(); d++) {
        double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
        double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

        double manhattanI = c1 - c2;
        sqrDist += manhattanI * manhattanI;
      }
      return new DoubleDistance(Math.sqrt(sqrDist));
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?, ?>> extends AbstractIndexBasedDistanceFunction.Parameterizer<LocalProjectionIndex.Factory<V, FilteredLocalPCAIndex<V>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, LocalProjectionIndex.Factory.class, KNNQueryFilteredPCAIndex.Factory.class);
    }

    @Override
    protected LocallyWeightedDistanceFunction<V> makeInstance() {
      return new LocallyWeightedDistanceFunction<V>(factory);
    }
  }
}