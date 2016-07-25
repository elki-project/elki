package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;

/**
 * Settings for the RdKNN Tree.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public class RdkNNSettings<O extends NumberVector> extends RTreeSettings {
  /**
   * Parameter k.
   */
  int k_max;

  /**
   * The distance function.
   */
  SpatialPrimitiveDistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   *
   * @param k_max Maximum k to support
   * @param distanceFunction Distance function
   */
  public RdkNNSettings(int k_max, SpatialPrimitiveDistanceFunction<? super O> distanceFunction) {
    super();
    this.k_max = k_max;
    this.distanceFunction = distanceFunction;
  }
}
