package de.lmu.ifi.dbs.elki.data.model;

import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;

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
 * Model for clusters produced by the data set generator.
 * 
 * @author Erich Schubert
 */
// TODO: intended include cluster size, discards, etc.?
public class GeneratorModel implements Model {
  /**
   * Affine transformation applied
   */
  AffineTransformation transform;
  
  /**
   * Individual distributions
   */
  List<Distribution> distributions;

  /**
   * Constructor.
   *
   * @param transform Transformation
   * @param distributions Distributions
   */
  public GeneratorModel(AffineTransformation transform, List<Distribution> distributions) {
    super();
    assert(transform == null || transform.getDimensionality() == distributions.size());
    this.transform = transform;
    this.distributions = distributions;
  }

  /**
   * @return the transform. May be null!
   */
  protected AffineTransformation getTransform() {
    return transform;
  }

  /**
   * Get the individual distributions
   * 
   * @return the nth distribution (starting at 1)
   */
  protected Distribution getDistribution(int dim) {
    return distributions.get(dim - 1);
  }
}
