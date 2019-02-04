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
package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;

/**
 * This is a rather naive rectangle arrangement class. It will try to place
 * rectangles on a canvas while maintaining the canvas size ratio as good as
 * possible. It does not do an exhaustive search for optimizing the layout, but
 * a greedy placement strategy, extending the canvas as little as possible.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <T> Key type
 */
public class RectangleArranger<T> {
  /**
   * Logging class
   */
  private static final Logging LOG = Logging.getLogger(RectangleArranger.class);

  /**
   * Target height/width ratio
   */
  private double ratio = 1.0;

  /**
   * Width
   */
  private double twidth = 1.0;

  /**
   * Height
   */
  private double theight = 1.0;

  /**
   * Column widths
   */
  private DoubleArray widths = new DoubleArray();

  /**
   * Column heights
   */
  private DoubleArray heights = new DoubleArray();

  /**
   * Map indicating which cells are used.
   */
  private ArrayList<ArrayList<Object>> usage = new ArrayList<>();

  /**
   * Data
   */
  private Map<T, double[]> map = new HashMap<>();

  /**
   * Constructor.
   *
   * @param ratio
   */
  public RectangleArranger(double ratio) {
    this(ratio, 1.0);
  }

  /**
   * Constructor.
   *
   * @param width Canvas width
   * @param height Canvas height
   */
  public RectangleArranger(double width, double height) {
    this.ratio = width / height;
    this.twidth = width;
    this.theight = height;
    this.widths.add(width);
    this.heights.add(height);
    // setup usage matrix
    ArrayList<Object> u = new ArrayList<>();
    u.add(null);
    this.usage.add(u);
    assertConsistent();
  }

  /**
   * Add a new recangle.
   *
   * @param w Width
   * @param h Height
   * @param data Data object to add (key)
   */
  public void put(double w, double h, T data) {
    if(LOG.isDebuggingFinest()) {
      LOG.finest("Add: " + w + "x" + h);
    }
    final int cols = widths.size();
    final int rows = heights.size();

    int bestsx = -1;
    int bestsy = -1;
    int bestex = cols - 1;
    int bestey = -1;
    double bestwi;
    double besthi;
    double bestinc;
    // Baseline: grow by adding to the top or to the right.
    {
      double i1 = computeIncreaseArea(w, Math.max(0, h - theight));
      double i2 = computeIncreaseArea(Math.max(0, w - twidth), h);
      if(i1 < i2) {
        bestwi = w;
        besthi = Math.max(0, h - theight);
        bestinc = i1;
      }
      else {
        bestwi = Math.max(0, w - twidth);
        besthi = h;
        bestinc = i2;
      }
    }
    // Find position with minimum increase
    for(int sy = 0; sy < rows; sy++) {
      for(int sx = 0; sx < cols; sx++) {
        if(usage.get(sy).get(sx) != null) {
          continue;
        }
        // Start with single cell
        double avw = widths.get(sx);
        double avh = heights.get(sy);
        int ex = sx;
        int ey = sy;
        while(avw < w || avh < h) {
          // Grow width first
          if(avw / avh < w / h) {
            if(avw < w && ex + 1 < cols) {
              boolean ok = true;
              // All unused?
              for(int y = sy; y <= ey; y++) {
                if(usage.get(y).get(ex + 1) != null) {
                  ok = false;
                }
              }
              if(ok) {
                ex += 1;
                avw += widths.get(ex);
                continue;
              }
            }
            if(avh < h && ey + 1 < rows) {
              boolean ok = true;
              // All unused?
              for(int x = sx; x <= ex; x++) {
                if(usage.get(ey + 1).get(x) != null) {
                  ok = false;
                }
              }
              if(ok) {
                ey += 1;
                avh += heights.get(ey);
                continue;
              }
            }
          }
          else { // Grow height first
            if(avh < h && ey + 1 < rows) {
              boolean ok = true;
              // All unused?
              for(int x = sx; x <= ex; x++) {
                if(usage.get(ey + 1).get(x) != null) {
                  ok = false;
                }
              }
              if(ok) {
                ey += 1;
                avh += heights.get(ey);
                continue;
              }
            }
            if(avw < w && ex + 1 < cols) {
              boolean ok = true;
              // All unused?
              for(int y = sy; y <= ey; y++) {
                if(usage.get(y).get(ex + 1) != null) {
                  ok = false;
                }
              }
              if(ok) {
                ex += 1;
                avw += widths.get(ex);
                continue;
              }
            }
          }
          break;
        }
        // Good match, or extension possible?
        if(avw < w && ex < cols - 1) {
          continue;
        }
        if(avh < h && ey < rows - 1) {
          continue;
        }
        // Compute increase:
        double winc = Math.max(0.0, w - avw);
        double hinc = Math.max(0.0, h - avh);
        double inc = computeIncreaseArea(winc, hinc);

        if(LOG.isDebuggingFinest()) {
          LOG.debugFinest("Candidate: " + sx + "," + sy + " - " + ex + "," + ey + ": " + avw + "x" + avh + " " + inc);
        }
        if(inc < bestinc) {
          bestinc = inc;
          bestsx = sx;
          bestsy = sy;
          bestex = ex;
          bestey = ey;
          bestwi = w - avw;
          besthi = h - avh;
        }
        if(inc == 0) {
          // Can't find better
          // TODO: try to do less splitting maybe?
          break;
        }
      }
      assert assertConsistent();
    }
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("Best: " + bestsx + "," + bestsy + " - " + bestex + "," + bestey + " inc: " + bestwi + "x" + besthi + " " + bestinc);
    }
    // Need to increase the total area
    if(bestinc > 0) {
      assert(bestex == cols - 1 || bestey == rows - 1);
      double inc = Math.max(bestwi, besthi * ratio);
      resize(inc);

      // Resubmit
      put(w, h, data);
      return;
    }
    // Need to split a column.
    // TODO: find best column to split. Currently: last
    if(bestwi < 0.0) {
      splitCol(bestex, -bestwi);
      bestwi = 0.0;
    }
    // Need to split a row.
    // TODO: find best row to split. Currently: last
    if(besthi < 0.0) {
      splitRow(bestey, -besthi);
      besthi = 0.0;
    }
    for(int x = bestsx; x <= bestex; x++) {
      for(int y = bestsy; y <= bestey; y++) {
        usage.get(y).set(x, data);
      }
    }
    double xpos = 0.0;
    double ypos = 0.0;
    {
      for(int x = 0; x < bestsx; x++) {
        xpos += widths.get(x);
      }
      for(int y = 0; y < bestsy; y++) {
        ypos += heights.get(y);
      }
    }
    map.put(data, new double[] { xpos, ypos, w, h });
    if(LOG.isDebuggingFinest()) {
      logSizes();
    }
  }

  protected double computeIncreaseArea(double winc, double hinc) {
    double inc = Math.max(winc, hinc * ratio);
    inc = inc * (hinc + inc / ratio + winc / ratio);
    return inc;
  }

  protected void splitRow(int bestey, double besthi) {
    assert(bestey < heights.size());
    if(heights.get(bestey) - besthi <= Double.MIN_NORMAL) {
      return;
    }
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Split row " + bestey);
    }
    heights.insert(bestey + 1, besthi);
    heights.set(bestey, heights.get(bestey) - besthi);
    // Update used map
    usage.add(bestey + 1, new ArrayList<>(usage.get(bestey)));
  }

  protected void splitCol(int bestex, double bestwi) {
    assert(bestex < widths.size());
    if(widths.get(bestex) - bestwi <= Double.MIN_NORMAL) {
      return;
    }
    final int rows = heights.size();
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Split column " + bestex);
    }
    widths.insert(bestex + 1, bestwi);
    widths.set(bestex, widths.get(bestex) - bestwi);
    // Update used map
    for(int y = 0; y < rows; y++) {
      usage.get(y).add(bestex + 1, usage.get(y).get(bestex));
    }
    assert assertConsistent();
  }

  private void resize(double inc) {
    final int cols = widths.size();
    final int rows = heights.size();
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Resize by " + inc + "x" + (inc / ratio));
      if(LOG.isDebuggingFinest()) {
        logSizes();
      }
    }
    // TODO: if the last row or column is empty, we can do this simpler
    widths.add(inc);
    twidth += inc;
    heights.add(inc / ratio);
    theight += inc / ratio;
    // Add column:
    for(int y = 0; y < rows; y++) {
      usage.get(y).add(null);
    }
    // Add row:
    {
      ArrayList<Object> row = new ArrayList<>();
      for(int x = 0; x <= cols; x++) {
        row.add(null);
      }
      usage.add(row);
    }
    assert assertConsistent();
    if(LOG.isDebuggingFinest()) {
      logSizes();
    }
  }

  /**
   * Get the position data of the object
   *
   * @param object Query object
   * @return Position information: x,y,w,h
   */
  public double[] get(T object) {
    double[] v = map.get(object);
    if(v == null) {
      return null;
    }
    return v.clone();
  }

  private boolean assertConsistent() {
    final int cols = widths.size();
    final int rows = heights.size();
    {
      double wsum = 0.0;
      for(int x = 0; x < cols; x++) {
        assert(widths.get(x) > 0) : "Non-positive width: " + widths.get(x) + " at " + x;
        wsum += widths.get(x);
      }
      assert(Math.abs(wsum - twidth) < 1E-10);
    }
    {
      double hsum = 0.0;
      for(int y = 0; y < rows; y++) {
        assert(heights.get(y) > 0) : "Non-positive height: " + heights.get(y) + " at " + y;
        hsum += heights.get(y);
      }
      assert(Math.abs(hsum - theight) < 1E-10);
    }
    {
      assert(usage.size() == rows);
      for(int y = 0; y < rows; y++) {
        assert(usage.get(y).size() == cols);
      }
    }
    return true;
  }

  /**
   * Debug logging
   */
  protected void logSizes() {
    StringBuilder buf = new StringBuilder(1000);
    final int cols = widths.size();
    final int rows = heights.size();
    {
      buf.append("Widths: ");
      for(int x = 0; x < cols; x++) {
        if(x > 0) {
          buf.append(", ");
        }
        buf.append(widths.get(x));
      }
      buf.append('\n');
    }
    {
      buf.append("Heights: ");
      for(int y = 0; y < rows; y++) {
        if(y > 0) {
          buf.append(", ");
        }
        buf.append(heights.get(y));
      }
      buf.append('\n');
    }
    {
      for(int y = 0; y < rows; y++) {
        for(int x = 0; x < cols; x++) {
          buf.append(usage.get(y).get(x) != null ? 'X' : '_');
        }
        buf.append("|\n");
      }
      for(int x = 0; x < cols; x++) {
        buf.append('-');
      }
      buf.append("+\n");
    }
    LOG.debug(buf);
  }

  /**
   * Compute the relative fill. Useful for triggering a relayout if the relative
   * fill is not satisfactory.
   *
   * @return relative fill
   */
  public double relativeFill() {
    double acc = 0.0;
    final int cols = widths.size();
    final int rows = heights.size();
    {
      for(int y = 0; y < rows; y++) {
        for(int x = 0; x < cols; x++) {
          if(usage.get(y).get(x) != null) {
            acc += widths.get(x) * heights.get(y);
          }
        }
      }
    }
    return acc / (twidth * theight);
  }

  /**
   * Get the total canvas width
   *
   * @return Width
   */
  public double getWidth() {
    return twidth;
  }

  /**
   * Get the total canvas height
   *
   * @return Height
   */
  public double getHeight() {
    return theight;
  }

  /**
   * The items contained in the map.
   *
   * @return entry set
   */
  public Set<Entry<T, double[]>> entrySet() {
    return Collections.unmodifiableSet(map.entrySet());
  }

  /**
   * The item keys contained in the map.
   *
   * @return key set
   */
  public Set<T> keySet() {
    return Collections.unmodifiableSet(map.keySet());
  }

  /**
   * Test method.
   *
   * @param args
   */
  public static void main(String[] args) {
    LoggingConfiguration.setLevelFor(RectangleArranger.class.getName(), Level.FINEST.getName());
    RectangleArranger<String> r = new RectangleArranger<>(1.3);
    r.put(4., 1., "Histogram");
    r.put(4., 4., "3D view");
    r.put(1., 1., "Meta 1");
    r.put(1., 1., "Meta 2");
    r.put(1., 1., "Meta 3");
    r.put(2., 2., "Meta 4");
    r.put(2., 2., "Meta 5");

    r = new RectangleArranger<>(3., 3.);
    r.put(1., 2., "A");
    r.put(2., 1., "B");
    r.put(1., 2., "C");
    r.put(2., 1., "D");
    r.put(2., 2., "E");

    r = new RectangleArranger<>(4 - 2.6521739130434785);
    r.put(4., .5, "A");
    r.put(4., 3., "B");
    r.put(4., 1., "C");
    r.put(1., .1, "D");
  }
}