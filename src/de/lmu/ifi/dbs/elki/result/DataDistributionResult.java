package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.histograms.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

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
 * This result wraps a number of simple statistics on the data per dimension,
 * such as minimum and maximum values, sensible scales for this and histograms.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DoubleMinMax
 * @apiviz.composedOf LinearScale
 * @apiviz.composedOf AggregatingHistogram
 */
public class DataDistributionResult extends BasicResult {
  /**
   * Minimum and maximum of each dimension.
   */
  private ArrayList<DoubleMinMax> minmax;

  /**
   * The scales for each axis
   */
  private ArrayList<LinearScale> scales;

  /**
   * Value Histogram
   */
  private ArrayList<? extends AggregatingHistogram<Double, Double>> histograms;

  /**
   * Constructor.
   * 
   * @param minmax Minimum and maximum
   * @param scales Scales to use
   * @param histograms Data histogram (unscaled)
   */
  public DataDistributionResult(ArrayList<DoubleMinMax> minmax, ArrayList<LinearScale> scales, ArrayList<? extends AggregatingHistogram<Double, Double>> histograms) {
    super("Data distribution", "data-distribution");
    this.minmax = minmax;
    this.scales = scales;
    this.histograms = histograms;
  }

  /**
   * Get actual data minimum
   * 
   * @param dim Dimension to use.
   * @return
   */
  public double getActualMin(int dim) {
    return minmax.get(dim).getMin();
  }

  /**
   * Get actual data maximum
   * 
   * @param dim Dimension to use.
   * @return
   */
  public double getActualMax(int dim) {
    return minmax.get(dim).getMax();
  }

  /**
   * Get scaling function for the given dimension.
   * 
   * @param dim Dimension
   * @return Scale
   */
  public LinearScale getScale(int dim) {
    return scales.get(dim);
  }

  /**
   * Access the data histogram
   * 
   * @param dim Dimension
   * @return Iterator for the histogram
   */
  public Iterator<DoubleObjPair<Double>> iterHistogram(int dim) {
    return histograms.get(dim).iterator();
  }
}