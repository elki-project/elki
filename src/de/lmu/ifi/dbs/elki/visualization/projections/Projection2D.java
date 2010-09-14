package de.lmu.ifi.dbs.elki.visualization.projections;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Projections that have specialized methods to only compute the first two
 * dimensions of the projection.
 * 
 * @author Erich Schubert
 */
public interface Projection2D extends Projection {
  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double[] fastProjectScaledToRender(Vector v);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeDataToRenderSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeScaledToRender(Vector v);

  /**
   * Estimate the viewport requirements
   * 
   * @return MinMax for x and y obtained from projecting scale endpoints
   */
  public Pair<MinMax<Double>, MinMax<Double>> estimateViewport();

  /**
   * Get a SVG transformation string to bring the contents into the unit cube.
   * 
   * @param margin extra margin to add.
   * @param width Width
   * @param height Height
   * @return transformation string.
   */
  public String estimateTransformString(double margin, double width, double height);

  /**
   * Get a bit set of dimensions that are visible.
   * 
   * @return Bit set, first dimension is bit 0.
   */
  public BitSet getVisibleDimensions2D();
}