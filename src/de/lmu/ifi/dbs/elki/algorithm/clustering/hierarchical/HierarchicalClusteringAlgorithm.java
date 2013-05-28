package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

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
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface for hierarchical clustering algorithms.
 * 
 * This interface allows the algorithms to be used by e.g.
 * {@link ExtractFlatClusteringFromHierarchy}.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PointerHierarchyRepresentationResult
 * 
 * @param <D> Distance type
 */
public interface HierarchicalClusteringAlgorithm<D extends Distance<D>> extends Algorithm {
  @Override
  public PointerHierarchyRepresentationResult<D> run(Database db);

  /**
   * Return the distance type that will be used by the algorithm.
   * 
   * @return Distance factory.
   */
  public D getDistanceFactory();
}
