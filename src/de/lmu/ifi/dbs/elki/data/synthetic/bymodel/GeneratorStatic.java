package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Class for static clusters, that is an implementation of GeneratorInterface
 * that will return only a given set of points.
 * 
 * @author Erich Schubert
 */
public class GeneratorStatic implements GeneratorInterface {
  /**
   * Cluster name
   */
  public String name;

  /**
   * Cluster points
   */
  public LinkedList<Vector> points;

  /**
   * Construct generator using given name and points
   * 
   * @param name Cluster name
   * @param points Cluster points
   */
  public GeneratorStatic(String name, LinkedList<Vector> points) {
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
  public List<Vector> generate(int count) {
    return new ArrayList<Vector>(points);
  }

  /**
   * Get density at a given coordinate.
   */
  @Override
  public double getDensity(Vector p) {
    for(Vector my : points) {
      if(my.equals(p)) {
        return Double.POSITIVE_INFINITY;
      }
    }
    return 0.0;
  }

  /**
   * Get cluster dimensionality
   */
  @Override
  public int getDim() {
    return points.getFirst().getDimensionality();
  }

  /**
   * Get number of discarded points
   * 
   * @return number of discarded points
   */
  public int getDiscarded() {
    return 0;
  }

  /**
   * Get cluster name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Get cluster points
   */
  @Override
  public List<Vector> getPoints() {
    return points;
  }

  /**
   * Get cluster size
   */
  @Override
  public int getSize() {
    return points.size();
  }

  /**
   * Notify cluster of discarded points. Not supported for static generators.
   * 
   * @param discarded parameter not supported.
   * @throws UnableToComplyException always thrown, since the static generator doesn't supprot discards.
   */
  public void setDiscarded(int discarded) throws UnableToComplyException {
    throw new UnableToComplyException("Points in static clusters may never be discarded.");
  }
}