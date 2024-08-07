/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.data.model;
import elki.result.textwriter.TextWriterStream;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class MeanModel extends SimplePrototypeModel<double[]> {
  boolean weighted = false;
  double weight;

  /**
   * Constructor with mean
   * 
   * @param mean Cluster mean
   */
  public MeanModel(double[] mean) {
    super(mean);
  }

  /**
   * Constructor with mean and weight
   * 
   * @param mean Cluster mean
   * @param weight Weight
   */
  public MeanModel(double[] mean, double weight) {
    super(mean);
    this.weighted = true;
    this.weight = weight;
  }
  
    @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    if (weighted) {
      out.commentPrintLn("weight: " + weight);
    }
  }
  /**
   * Get the mean.
   * 
   * @return mean (do not modify!)
   */
  public double[] getMean() {
    return prototype;
  }

  /**
   * Get the weight.
   * 
   * @return weight
   */
  public double getWeight() {
    return weight;
  }

  @Override
  public String getPrototypeType() {
    return "Mean";
  }
}
