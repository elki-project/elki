package de.lmu.ifi.dbs.elki.math.statistics.dependence;

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
import de.lmu.ifi.dbs.elki.math.SinCosTable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Compute the similarity of dimensions by using a hough transformation.
 * 
 * Reference: <br>
 * <p>
 * A. Tatu, G. Albuquerque, M. Eisemann, P. Bak, H. Theisel, M. A. Magnor, and
 * D. A. Keim.<br />
 * Automated Analytical Methods to Support Visual Exploration of High-
 * Dimensional Data. <br/>
 * IEEEVisualization and Computer Graphics, 2011.
 * </p>
 * 
 * FIXME: This needs serious TESTING before release. Large parts have been
 * rewritten, but could not be tested at the time of rewriting.
 * 
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 */
@Reference(authors = "A. Tatu, G. Albuquerque, M. Eisemann, P. Bak, H. Theisel, M. A. Magnor, and D. A. Keim", //
title = "Automated Analytical Methods to Support Visual Exploration of High-Dimensional Data", //
booktitle = "IEEE Trans. Visualization and Computer Graphics, 2011", //
url = "http://dx.doi.org/10.1109/TVCG.2010.242")
public class HSMDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final HSMDependenceMeasure STATIC = new HSMDependenceMeasure();

  /**
   * Angular resolution. Best if divisible by 4: smaller tables.
   * 
   * The original publication used 50.
   */
  private final static int STEPS = 48; // 64;

  /**
   * Resolution of image.
   */
  private final int resolution = 512;

  /**
   * Precompute sinus and cosinus
   */
  private final static SinCosTable table = SinCosTable.make(STEPS);

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    boolean[][] pic = new boolean[resolution][resolution];

    // Get attribute value range:
    final double off1, scale1, off2, scale2;
    {
      double mi = adapter1.getDouble(data1, 0), ma = mi;
      for(int i = 1; i < len; ++i) {
        double v = adapter1.getDouble(data1, i);
        if(v < mi) {
          mi = v;
        }
        else if(v > ma) {
          ma = v;
        }
      }
      off1 = mi;
      scale1 = (ma > mi) ? (1. / (ma - mi)) : 1.;
      // Second data
      mi = adapter2.getDouble(data2, 0);
      ma = mi;
      for(int i = 1; i < len; ++i) {
        double v = adapter2.getDouble(data2, i);
        if(v < mi) {
          mi = v;
        }
        else if(v > ma) {
          ma = v;
        }
      }
      off2 = mi;
      scale2 = (ma > mi) ? (1. / (ma - mi)) : 1.;
    }
    // Iterate dataset
    for(int i = 0; i < len; ++i) {
      double xi = (adapter1.getDouble(data1, i) - off1) * scale1;
      double xj = (adapter2.getDouble(data2, i) - off2) * scale2;
      drawLine(0, (int) (resolution * xi), resolution - 1, (int) (resolution * xj), pic);
    }

    final double stepsq = (double) STEPS * (double) STEPS;
    int[][] hough = houghTransformation(pic);
    // The original publication said "median", but judging from the text,
    // meant "mean". Otherwise, always half of the cells are above the
    // threshold, which doesn't match the explanation there.
    double mean = sumMatrix(hough) / stepsq;
    int abovemean = countAboveThreshold(hough, mean);

    return 1. - (abovemean / stepsq);
  }

  /**
   * Compute the sum of a matrix.
   * 
   * @param mat Matrix
   * @return Sum of all elements
   */
  private long sumMatrix(int[][] mat) {
    long ret = 0;
    for(int i = 0; i < mat.length; i++) {
      final int[] row = mat[i];
      for(int j = 0; j < row.length; j++) {
        ret += row[j];
      }
    }
    return ret;
  }

  /**
   * Count the number of cells above the threshold.
   * 
   * @param mat Matrix
   * @param threshold Threshold
   * @return Number of elements above the threshold.
   */
  private int countAboveThreshold(int[][] mat, double threshold) {
    int ret = 0;
    for(int i = 0; i < mat.length; i++) {
      int[] row = mat[i];
      for(int j = 0; j < row.length; j++) {
        if(row[j] >= threshold) {
          ret++;
        }
      }
    }
    return ret;
  }

  /**
   * Perform a hough transformation on the binary image in "mat".
   * 
   * @param mat Binary image
   * @return Hough transformation of image.
   */
  private int[][] houghTransformation(boolean[][] mat) {
    final int xres = mat.length, yres = mat[0].length;
    final double tscale = STEPS * .5 / (xres + yres);
    final int[][] ret = new int[STEPS][STEPS];

    for(int x = 0; x < mat.length; x++) {
      for(int y = 0; y < mat[0].length; y++) {
        if(mat[x][y]) {
          for(int i = 0; i < STEPS; i++) {
            final int d = (STEPS >> 1) + (int) (tscale * (x * table.cos(i) + y * table.sin(i)));
            if(d > 0 && d < STEPS) {
              ret[d][i]++;
            }
          }
        }
      }
    }

    return ret;
  }

  /**
   * Draw a line onto the array, using the classic Bresenham algorithm.
   * 
   * @param x0 Start X
   * @param y0 Start Y
   * @param x1 End X
   * @param y1 End Y
   * @param pic Picture array
   */
  private static void drawLine(int x0, int y0, int x1, int y1, boolean[][] pic) {
    final int xres = pic.length, yres = pic[0].length;
    // Ensure bounds
    y0 = (y0 < 0) ? 0 : (y0 >= yres) ? (yres - 1) : y0;
    y1 = (y1 < 0) ? 0 : (y1 >= yres) ? (yres - 1) : y1;
    x0 = (x0 < 0) ? 0 : (x0 >= xres) ? (xres - 1) : x0;
    x1 = (x1 < 0) ? 0 : (x1 >= xres) ? (xres - 1) : x1;
    // Default slope
    final int dx = +Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
    final int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
    // Error counter
    int err = dx + dy;

    for(;;) {
      pic[x0][y0] = true;
      if(x0 == x1 && y0 == y1) {
        break;
      }

      final int e2 = err << 1;
      if(e2 > dy) {
        err += dy;
        x0 += sx;
      }
      if(e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HSMDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
