/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;

/**
 * Class for static clusters, that is an implementation of GeneratorInterface
 * that will return only a given set of points.
 *
 * @author Erich Schubert
 * @since 0.2
 */
public class GeneratorStatic implements GeneratorInterface {
  /**
   * Cluster name
   */
  public String name;

  /**
   * Cluster points
   */
  public List<double[]> points;

  /**
   * Construct generator using given name and points
   *
   * @param name Cluster name
   * @param points Cluster points
   */
  public GeneratorStatic(String name, List<double[]> points) {
    super();
    this.name = name;
    this.points = points;
  }

  /**
   * "Generate" new cluster points. Static generators always return their
   * predefined set of points.
   *
   * @param count parameter is ignored.
   */
  @Override
  public List<double[]> generate(int count) {
    return Collections.unmodifiableList(points);
  }

  @Override
  public double getDensity(double[] p) {
    for(double[] my : points) {
      if(Arrays.equals(my, p)) {
        return Double.POSITIVE_INFINITY;
      }
    }
    return 0.0;
  }

  @Override
  public int getDim() {
    return points.get(0).length;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getSize() {
    return points.size();
  }

  @Override
  public Model makeModel() {
    return ClusterModel.CLUSTER;
  }

  @Override
  public double[] computeMean() {
    // Not supported except for singletons.
    return points.size() == 1 ? points.get(1) : null;
  }
}