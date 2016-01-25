package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;

/**
 * Models useable in EM clustering.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public interface EMClusterModel<M extends MeanModel> {
  /**
   * Begin the E step.
   */
  void beginEStep();

  /**
   * Update the
   * 
   * @param vec Vector to process
   * @param weight Weight
   */
  void updateE(NumberVector vec, double weight);

  /**
   * Finalize the E step.
   */
  void finalizeEStep();

  /**
   * Estimate the likelihood of a vector.
   * 
   * @param vec Vector
   * @return Likelihood.
   */
  double estimateDensity(NumberVector vec);

  /**
   * Finalize a cluster model.
   * 
   * @return Cluster model
   */
  M finalizeCluster();  

  /**
   * Get the cluster weight.
   * 
   * @return Cluster weight
   */
  double getWeight();

  /**
   * Set the cluster weight.
   * 
   * @param weight Cluster weight
   */
  void setWeight(double weight);
}