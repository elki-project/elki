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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Distance function to proxy computations to another distance (that probably
 * was run before).
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public class ProxyDistanceFunction<O, D extends Distance<D>> extends AbstractDBIDDistanceFunction<D> {
  /**
   * Distance query
   */
  DistanceQuery<O, D> inner;

  /**
   * Constructor
   * 
   * @param inner Inner distance
   */
  public ProxyDistanceFunction(DistanceQuery<O, D> inner) {
    super();
    this.inner = inner;
  }
  
  /**
   * Static method version.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param inner Inner distance query
   * @return Proxy object
   */
  public static <O, D extends Distance<D>> ProxyDistanceFunction<O, D> proxy(DistanceQuery<O, D> inner) {
    return new ProxyDistanceFunction<O, D>(inner);
  }

  @Override
  public D distance(DBIDRef o1, DBIDRef o2) {
    return inner.distance(o1, o2);
  }

  @Override
  public D getDistanceFactory() {
    return inner.getDistanceFactory();
  }

  /**
   * Get the inner query
   * 
   * @return query
   */
  public DistanceQuery<O, D> getDistanceQuery() {
    return inner;
  }

  /**
   * @param inner the inner distance query to set
   */
  public void setDistanceQuery(DistanceQuery<O, D> inner) {
    this.inner = inner;
  }

  /**
   * Helper function, to resolve any wrapped Proxy Distances
   * 
   * @param <V> Object type
   * @param <D> Distance type
   * @param dfun Distance function to unwrap.
   * @return unwrapped distance function
   */
  @SuppressWarnings("unchecked")
  public static <V, T extends V, D extends Distance<D>> DistanceFunction<? super V, D> unwrapDistance(DistanceFunction<V, D> dfun) {
    if(ProxyDistanceFunction.class.isInstance(dfun)) {
      return unwrapDistance(((ProxyDistanceFunction<V, D>) dfun).getDistanceQuery().getDistanceFunction());
    }
    return dfun;
  }


  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    ProxyDistanceFunction<?, ?> other = (ProxyDistanceFunction<?, ?>) obj;
    return this.inner.equals(other.inner);
  }

  @Override
  public int hashCode() {
    return this.inner.hashCode();
  }
}