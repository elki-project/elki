package de.lmu.ifi.dbs.elki.distance.distancefunction.histogram;

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
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractSpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function based on histogram matching, i.e. Manhattan distance on the
 * cumulative density function.
 * 
 * This distance function assumes there exist a natural order in the vectors,
 * i.e. they should be some 1-dimensional histogram.
 * 
 * This is also known as Earth Movers Distance (EMD), 1st Mallows distance or
 * 1st Wasserstein metric (also Vasershtein metric), for the special case of a
 * one-dimensional histogram, where the cost is linear in the number of bins to
 * transport.
 * 
 * Reference:
 * <p>
 * L.N. Vaserstein<br />
 * Markov processes over denumerable products of spaces describing large systems
 * of automata <br />
 * Problemy Peredachi Informatsii 5.3 / Problems of Information Transmission 5:3
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "L.N. Vaserstein", //
title = "Markov processes over denumerable products of spaces describing large systems of automata", //
booktitle = "Problemy Peredachi Informatsii 5.3 / Problems of Information Transmission, 5:3", //
url = "http://mi.mathnet.ru/eng/ppi1811")
public class HistogramMatchDistanceFunction extends AbstractSpatialDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final HistogramMatchDistanceFunction STATIC = new HistogramMatchDistanceFunction();

  /**
   * Constructor for the Histogram match distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public HistogramMatchDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double xs = 0., ys = 0., agg = 0.;
    for(int i = 0; i < dim; i++) {
      xs += v1.doubleValue(i);
      ys += v2.doubleValue(i);
      agg += Math.abs(xs - ys);
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double xmin = 0., xmax = 0., ymin = 0., ymax = 0., agg = 0.;
    for(int i = 0; i < dim; i++) {
      xmin += mbr1.getMin(i);
      xmax += mbr1.getMax(i);
      ymin += mbr2.getMin(i);
      ymax += mbr2.getMax(i);
      agg += (ymin > xmax) ? (ymin - xmax) : (xmin > ymax) ? (xmin - ymax) : 0.;
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "HistogramMatchDistanceFunction";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HistogramMatchDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
