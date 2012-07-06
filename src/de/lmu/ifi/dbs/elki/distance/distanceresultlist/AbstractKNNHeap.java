package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

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
import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TiedTopBoundedHeap;

/**
 * Heap used for KNN management.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KNNList oneway - - serializes to
 * 
 * @param <P> pair type
 * @param <D> distance type
 */
public abstract class AbstractKNNHeap<P extends DistanceDBIDPair<D>, D extends Distance<D>> extends TiedTopBoundedHeap<P> implements KNNHeap<D> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Static comparator.
   */
  private static final Comparator<? super DistanceDBIDPair<?>> COMPARATOR = new Comp();

  /**
   * Constructor.
   * 
   * @param k k Parameter
   */
  public AbstractKNNHeap(int k) {
    super(k, COMPARATOR);
  }

  @Override
  @Deprecated
  public ArrayList<P> toSortedArrayList() {
    ArrayList<P> list = super.toSortedArrayList();
    Collections.reverse(list);
    return list;
  }

  /**
   * Get the K parameter ("maxsize" internally).
   * 
   * @return K
   */
  @Override
  public int getK() {
    return super.getMaxSize();
  }

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   * 
   * @return Maximum distance
   */
  @Override
  public D getKNNDistance() {
    if(size() < getK()) {
      return null;
    }
    return peek().getDistance();
  }

  /**
   * Comparator to use.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class Comp implements Comparator<DistanceDBIDPair<?>> {
    @SuppressWarnings("unchecked")
    @Override
    public int compare(DistanceDBIDPair<?> o1, DistanceDBIDPair<?> o2) {
      return -((DistanceDBIDPair<DoubleDistance>)o1).compareByDistance((DistanceDBIDPair<DoubleDistance>)o2);
    }
  }
}