package de.lmu.ifi.dbs.elki.data.images;

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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract class for color histogram computation.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ImageUtil
 */
public abstract class AbstractComputeColorHistogram implements ComputeColorHistogram {
  @Override
  public double[] computeColorHistogram(File file, File mask) throws IOException {
    BufferedImage image = ImageUtil.loadImage(file);
    int height = image.getHeight();
    int width = image.getWidth();

    BufferedImage maski = null;
    if(mask != null) {
      maski = ImageUtil.loadImage(mask);
      if(maski.getHeight() != height || maski.getWidth() != width) {
        throw new AbortException("Input image and mask do not agree on the image size!");
      }
    }

    double[] bins = new double[getNumBins()];
    long valid = 0;

    for(int x = 0; x < width; x++) {
      for(int y = 0; y < height; y++) {
        if(maski != null) {
          int col = maski.getRGB(x, y);
          // More transparent than covering
          if((col >>> 24) < 127) {
            continue;
          }
          // More black than white: (R+G+B) > 1.5 * 255
          if(((col >>> 16) & 0xFF) + ((col >>> 8) & 0xFF) + (col & 0xFF) < 382) {
            continue;
          }
        }
        int bin = getBinForColor(image.getRGB(x, y));
        assert (bin < bins.length);
        bins[bin] += 1;
        valid += 1;
      }
    }
    if (valid == 0) {
      throw new AbortException("Mask apparently was all-black.");
    }
    for(int i = 0; i < bins.length; i++) {
      bins[i] /= valid;
    }
    return bins;
  }

  /**
   * Get the number of bins.
   * 
   * @return Number of bins
   */
  protected abstract int getNumBins();

  /**
   * Compute the bin number from a pixel color value.
   * 
   * @param rgb Pixel color value
   * @return Bin number
   */
  protected abstract int getBinForColor(int rgb);
}