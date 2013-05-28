package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Encapsulates the attributes of an assignment during a split.
 * 
 * @author Elke Achtert
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
   * The first covering radius.
   */
  private double firstCoveringRadius;

  /**
   * The second covering radius.
   */
  private double secondCoveringRadius;

  /**
   * The assignments to the first routing object.
   */
  private List<DistanceEntry<E>> firstAssignments;

  /**
   * The assignments to the second routing object.
   */
  private List<DistanceEntry<E>> secondAssignments;

  /**
   * Provides an assignment during a split of an MTree node.
   * 
   * @param id1 the first routing object
   * @param id2 the second routing object
   * @param firstCoveringRadius the first covering radius
   * @param secondCoveringRadius the second covering radius
   * @param firstAssignments the assignments to the first routing object
   * @param secondAssignments the assignments to the second routing object
   */
  public Assignments(DBID id1, DBID id2, double firstCoveringRadius, double secondCoveringRadius, List<DistanceEntry<E>> firstAssignments, List<DistanceEntry<E>> secondAssignments) {
    this.id1 = id1;
    this.id2 = id2;
    this.firstCoveringRadius = firstCoveringRadius;
    this.secondCoveringRadius = secondCoveringRadius;
    this.firstAssignments = firstAssignments;
    this.secondAssignments = secondAssignments;
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
   * Returns the first covering radius.
   * 
   * @return the first covering radius
   */
  public double getFirstCoveringRadius() {
    return firstCoveringRadius;
  }

  /**
   * Returns the second covering radius.
   * 
   * @return the second covering radius
   */
  public double getSecondCoveringRadius() {
    return secondCoveringRadius;
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
}
