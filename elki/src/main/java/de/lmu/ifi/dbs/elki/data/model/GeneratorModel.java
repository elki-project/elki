package de.lmu.ifi.dbs.elki.data.model;

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
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorInterface;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Cluster model for synthetically generated data.
 *
 * @author Erich Schubert
 */
public class GeneratorModel extends MeanModel {
  /**
   * Cluster generator.
   */
  private GeneratorInterface generator;

  /**
   * Constructor with mean.
   *
   * @param generator Cluster generator.
   * @param mean Mean vector.
   */
  public GeneratorModel(GeneratorInterface generator, Vector mean) {
    super(mean);
    this.generator = generator;
  }

  /**
   * Get the cluster generator.
   *
   * @return Cluster generator
   */
  public GeneratorInterface getGenerator() {
    return generator;
  }
}
