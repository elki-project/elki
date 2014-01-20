package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

/**
 * Distance function to proxy computations to another distance (that probably
 * was run before).
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 */
public class ProxyDistanceFunction<O> extends AbstractDBIDDistanceFunction {
  /**
   * Distance query
   */
  DistanceQuery<O> inner;

  /**
   * Constructor
   * 
   * @param inner Inner distance
   */
  public ProxyDistanceFunction(DistanceQuery<O> inner) {
    super();
    this.inner = inner;
  }
  
  /**
   * Static method version.
   * 
   * @param <O> Object type
   * @param inner Inner distance query
   * @return Proxy object
   */
  public static <O> ProxyDistanceFunction<O> proxy(DistanceQuery<O> inner) {
    return new ProxyDistanceFunction<>(inner);
  }

  @Override
  public double distance(DBIDRef o1, DBIDRef o2) {
    return inner.distance(o1, o2);
  }

  /**
   * Get the inner query
   * 
   * @return query
   */
  public DistanceQuery<O> getDistanceQuery() {
    return inner;
  }

  /**
   * @param inner the inner distance query to set
   */
  public void setDistanceQuery(DistanceQuery<O> inner) {
    this.inner = inner;
  }

  /**
   * Helper function, to resolve any wrapped Proxy Distances
   * 
   * @param <V> Object type
   * @param dfun Distance function to unwrap.
   * @return unwrapped distance function
   */
  @SuppressWarnings("unchecked")
  public static <V, T extends V> DistanceFunction<? super V> unwrapDistance(DistanceFunction<V> dfun) {
    if(ProxyDistanceFunction.class.isInstance(dfun)) {
      return unwrapDistance(((ProxyDistanceFunction<V>) dfun).getDistanceQuery().getDistanceFunction());
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
    ProxyDistanceFunction<?> other = (ProxyDistanceFunction<?>) obj;
    return this.inner.equals(other.inner);
  }

  @Override
  public int hashCode() {
    return this.inner.hashCode();
  }
}