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
package de.lmu.ifi.dbs.elki.math;

import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

/**
 * Class to precompute / cache Sinus and Cosinus values.
 * 
 * Note that the functions use integer offsets, not radians.
 * 
 * TODO: add an interpolation function.
 * 
 * TODO: add caching
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public abstract class SinCosTable {
  /**
   * Number of steps.
   */
  protected final int steps;

  /**
   * Constructor.
   * 
   * @param steps Number of steps (ideally, {@code steps % 4 = 0}!)
   */
  private SinCosTable(final int steps) {
    this.steps = steps;
  }

  /**
   * Get Cosine by step value.
   * 
   * @param step Step value
   * @return Cosinus
   */
  public abstract double cos(int step);

  /**
   * Get Sinus by step value.
   * 
   * @param step Step value
   * @return Sinus
   */
  public abstract double sin(int step);

  /**
   * Table that can't exploit much symmetry, because the steps are not divisible
   * by 2.
   * 
   * @author Erich Schubert
   */
  private static class FullTable extends SinCosTable {
    /**
     * Data store
     */
    private final double[] costable;

    /**
     * Data store
     */
    private final double[] sintable;

    /**
     * Constructor for tables with
     * 
     * @param steps
     */
    public FullTable(int steps) {
      super(steps);
      final double radstep = Math.toRadians(360. / steps);
      this.costable = new double[steps];
      this.sintable = new double[steps];
      double ang = 0.;
      final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
      for (int i = 0; i < steps; i++, ang += radstep) {
        this.sintable[i] = FastMath.sinAndCos(ang, tmp);
        this.costable[i] = tmp.value;
      }
    }

    /**
     * Get Cosine by step value.
     * 
     * @param step Step value
     * @return Cosinus
     */
    @Override
    public double cos(int step) {
      step = Math.abs(step) % steps;
      return costable[step];
    }

    /**
     * Get Sinus by step value.
     * 
     * @param step Step value
     * @return Sinus
     */
    @Override
    public double sin(int step) {
      step = step % steps;
      if (step < 0) {
        step += steps;
      }
      return sintable[step];
    }
  }

  /**
   * Table that exploits just one symmetry, as the number of steps is divisible
   * by two.
   * 
   * @author Erich Schubert
   */
  private static class HalfTable extends SinCosTable {
    /**
     * Number of steps div 2
     */
    private final int halfsteps;

    /**
     * Data store
     */
    private final double[] costable;

    /**
     * Data store
     */
    private final double[] sintable;

    /**
     * Constructor for tables with
     * 
     * @param steps
     */
    public HalfTable(int steps) {
      super(steps);
      this.halfsteps = steps >> 1;
      final double radstep = Math.toRadians(360. / steps);
      this.costable = new double[halfsteps + 1];
      this.sintable = new double[halfsteps + 1];
      double ang = 0.;
      for (int i = 0; i < halfsteps + 1; i++, ang += radstep) {
        this.costable[i] = FastMath.cos(ang);
        this.sintable[i] = FastMath.sin(ang);
      }
    }

    /**
     * Get Cosine by step value.
     * 
     * @param step Step value
     * @return Cosinus
     */
    @Override
    public double cos(int step) {
      // Tabularizing cosine is a bit more straightforward than sine
      // As we can just drop the sign here:
      step = Math.abs(step) % steps;
      if (step < costable.length) {
        return costable[step];
      }
      // Symmetry at PI:
      return costable[steps - step];
    }

    /**
     * Get Sinus by step value.
     * 
     * @param step Step value
     * @return Sinus
     */
    @Override
    public double sin(int step) {
      step = step % steps;
      if (step < 0) {
        step += steps;
      }
      if (step < sintable.length) {
        return sintable[step];
      }
      // Anti symmetry at PI:
      return -sintable[steps - step];
    }
  }

  /**
   * Table that exploits both symmetries, as the number of steps is divisible by
   * four.
   * 
   * @author Erich Schubert
   */
  private static class QuarterTable extends SinCosTable {
    /**
     * Number of steps div 4
     */
    private final int quarsteps;

    /**
     * Number of steps div 2
     */
    private final int halfsteps;

    /**
     * Data store
     */
    private final double[] costable;

    /**
     * Constructor for tables with
     * 
     * @param steps
     */
    public QuarterTable(int steps) {
      super(steps);
      this.halfsteps = steps >> 1;
      this.quarsteps = steps >> 2;
      final double radstep = Math.toRadians(360. / steps);
      this.costable = new double[quarsteps + 1];
      double ang = 0.;
      for (int i = 0; i < quarsteps + 1; i++, ang += radstep) {
        this.costable[i] = FastMath.cos(ang);
      }
    }

    /**
     * Get Cosine by step value.
     * 
     * @param step Step value
     * @return Cosinus
     */
    @Override
    public double cos(int step) {
      // Tabularizing cosine is a bit more straightforward than sine
      // As we can just drop the sign here:
      step = Math.abs(step) % steps;
      if (step < costable.length) {
        return costable[step];
      }
      // Symmetry at PI:
      if (step > halfsteps) {
        step = steps - step;
        if (step < costable.length) {
          return costable[step];
        }
      }
      // Inverse symmetry at PI/2:
      step = halfsteps - step;
      return -costable[step];
    }

    /**
     * Get Sinus by step value.
     * 
     * @param step Step value
     * @return Sinus
     */
    @Override
    public double sin(int step) {
      return -cos(step + quarsteps);
    }
  }

  /**
   * Make a table for the given number of steps.
   * 
   * For step numbers divisible by 4, an optimized implementation will be used.
   * 
   * @param steps Number of steps
   * @return Table
   */
  public static SinCosTable make(int steps) {
    if ((steps & 0x3) == 0) {
      return new QuarterTable(steps);
    }
    if ((steps & 0x1) == 0) {
      return new HalfTable(steps);
    }
    return new FullTable(steps);
  }
}
