package de.lmu.ifi.dbs.elki.index.tree.query;
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


/**
 * Candidate for expansion in a distance search (generic implementation).
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceSearchCandidate implements Comparable<DoubleDistanceSearchCandidate> {
  /**
   * Distance value
   */
  public double mindist;

  /**
   * Page id
   */
  public Integer nodeID;

  /**
   * Constructor.
   * 
   * @param mindist The minimum distance to this candidate
   * @param pagenr The page number of this candidate
   */
  public DoubleDistanceSearchCandidate(final double mindist, final Integer pagenr) {
    super();
    this.mindist = mindist;
    this.nodeID = pagenr;
  }

  @Override
  public boolean equals(Object obj) {
    final DoubleDistanceSearchCandidate other = (DoubleDistanceSearchCandidate) obj;
    return this.nodeID == other.nodeID;
  }

  @Override
  public int compareTo(DoubleDistanceSearchCandidate o) {
    return Double.compare(this.mindist, o.mindist);
  }
}