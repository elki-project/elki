package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

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

import de.lmu.ifi.dbs.elki.math.MeanVariance;

/**
 * Histogram class storing MeanVaraince object.
 * 
 * The histogram will start with "bin" bins, but it can grow dynamically to the
 * left and right.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf MeanVariance
 */
public class MeanVarianceStaticHistogram extends AbstractObjStaticHistogram<MeanVariance> {
  /**
   * Constructor.
   * 
   * @param bins Number of bins
   * @param min Cover minimum
   * @param max Cover maximum
   */
  public MeanVarianceStaticHistogram(int bins, double min, double max) {
    super(bins, min, max);
  }

  /**
   * Data store
   */
  MeanVariance[] data;

  /**
   * Update the value of a bin with new data.
   * 
   * @param coord Coordinate
   * @param val Value
   */
  public void put(double coord, double val) {
    get(coord).put(val);
  }

  /**
   * {@inheritDoc}
   * 
   * Data is combined by using {@link MeanVariance#put(MeanVariance)}.
   */
  @Override
  public void putData(double coord, MeanVariance data) {
    get(coord).put(data);
  }

  
  @Override
  protected MeanVariance makeObject() {
    return new MeanVariance();
  }
}
