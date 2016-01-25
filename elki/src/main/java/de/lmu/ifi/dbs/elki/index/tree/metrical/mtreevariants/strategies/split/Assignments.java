package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Encapsulates the attributes of an assignment during a split.
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @apiviz.composedOf DistanceEntry
 * 
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
public class Assignments<E extends MTreeEntry> {
  /**
   * The id of the first routing object.
   */
  private DBID id1;

  /**
   * The id of the second routing object.
   */
  private DBID id2;

  /**
   * The assignments to the first routing object.
   */
  private List<DistanceEntry<E>> firstAssignments;

  /**
   * The assignments to the second routing object.
   */
  private List<DistanceEntry<E>> secondAssignments;

  /**
   * Constructor.
   * 
   * @param id1 the first routing object
   * @param id2 the second routing object
   * @param size Maximum number of entries per list
   */
  public Assignments(DBID id1, DBID id2, int size) {
    this.id1 = id1;
    this.id2 = id2;
    this.firstAssignments = new ArrayList<>(size);
    this.secondAssignments = new ArrayList<>(size);
  }

  /**
   * Constructor.
   * 
   * @param size Maximum number of entries per list
   */
  public Assignments(int size) {
    this.id1 = null;
    this.id2 = null;
    this.firstAssignments = new ArrayList<>(size);
    this.secondAssignments = new ArrayList<>(size);
  }

  /**
   * Add an entry to the first set.
   * 
   * @param ent Entry
   * @param dist Distance
   * @param pos Position in parent
   */
  public void addToFirst(E ent, double dist, int pos) {
    firstAssignments.add(new DistanceEntry<>(ent, dist, pos));
  }

  /**
   * Compute the covering radius of the first assignment.
   * 
   * @param leaf {@code true} if in leaf mode.
   * @return Covering radius.
   */
  public double computeFirstCover(boolean leaf) {
    double max = 0.;
    for(DistanceEntry<E> e : firstAssignments) {
      double cover = leaf ? e.getDistance() : (e.getEntry().getCoveringRadius() + e.getDistance());
      max = cover > max ? cover : max;
    }
    return max;
  }

  /**
   * Compute the covering radius of the second assignment.
   * 
   * @param leaf {@code true} if in leaf mode.
   * @return Covering radius.
   */
  public double computeSecondCover(boolean leaf) {
    double max = 0.;
    for(DistanceEntry<E> e : secondAssignments) {
      double cover = leaf ? e.getDistance() : (e.getEntry().getCoveringRadius() + e.getDistance());
      max = cover > max ? cover : max;
    }
    return max;
  }

  /**
   * Add an entry to the second set.
   * 
   * @param ent Entry
   * @param dist Distance
   * @param pos Position in parent
   */
  public void addToSecond(E ent, double dist, int pos) {
    secondAssignments.add(new DistanceEntry<>(ent, dist, pos));
  }

  /**
   * Returns the id of the first routing object.
   * 
   * @return the id of the first routing object
   */
  public DBID getFirstRoutingObject() {
    return id1;
  }

  /**
   * Returns the id of the second routing object.
   * 
   * @return the id of the second routing object
   */
  public DBID getSecondRoutingObject() {
    return id2;
  }

  /**
   * Returns the assignments to the first routing object.
   * 
   * @return the assignments to the first routing object
   */
  public List<DistanceEntry<E>> getFirstAssignments() {
    return firstAssignments;
  }

  /**
   * Returns the assignments to the second routing object.
   * 
   * @return the assignments to the second routing object
   */
  public List<DistanceEntry<E>> getSecondAssignments() {
    return secondAssignments;
  }

  /**
   * Set the first routing object.
   * 
   * @param routingObjectID First routing object
   */
  public void setFirstRoutingObject(DBID routingObjectID) {
    this.id1 = routingObjectID;
  }

  /**
   * Set the second routing object.
   * 
   * @param routingObjectID Second routing object
   */
  public void setSecondRoutingObject(DBID routingObjectID) {
    this.id2 = routingObjectID;
  }
}
