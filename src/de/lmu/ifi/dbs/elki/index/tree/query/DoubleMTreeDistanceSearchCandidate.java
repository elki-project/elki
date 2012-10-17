package de.lmu.ifi.dbs.elki.index.tree.query;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Encapsulates the attributes for a object that can be stored in a heap. The
 * object to be stored represents a node in a M-Tree and some additional
 * information. Additionally to the regular expansion candidate, this object
 * holds the id of the routing object of the underlying M-Tree node and its
 * covering radius.
 * 
 * @author Elke Achtert
 */
public class DoubleMTreeDistanceSearchCandidate extends DoubleDistanceSearchCandidate {
  /**
   * The id of the routing object.
   */
  public DBID routingObjectID;

  /**
   * The distance from the query object to the routing object
   */
  public double routingDistance;

  /**
   * Creates a new heap node with the specified parameters.
   * 
   * @param mindist the minimum distance of the node
   * @param nodeID the id of the node
   * @param routingObjectID the id of the routing object of the node
   * @param routingDistance the distance from the query object to the query
   *        object
   */
  public DoubleMTreeDistanceSearchCandidate(final double mindist, final Integer nodeID, final DBID routingObjectID, double routingDistance) {
    super(mindist, nodeID);
    this.routingObjectID = routingObjectID;
    this.routingDistance = routingDistance;
  }
}
