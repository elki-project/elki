package de.lmu.ifi.dbs.elki.visualization;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;

/**
 * Class to encapsulate a projection as used in SVG plotting and UI, which will
 * often be just 2D, but should ideally support any projection onto a 2D plane.
 * 
 * @author Erich Schubert
 */
public class VisualizationProjection {
  /**
   * Database dimensionality
   */
  private int dim;

  /**
   * Scales in data set
   */
  private LinearScale[] scales;

  /**
   * Affine transformation used in projection
   */
  private AffineTransformation proj;

  /**
   * Constructor with a given database and axes.
   * 
   * @param db Database
   * @param scales Scales to use
   * @param ax1 First axis
   * @param ax2 Second axis
   */
  public VisualizationProjection(Database<? extends NumberVector<?,?>> db, LinearScale[] scales, int ax1, int ax2) {
    this(db, scales, axisProjection(db.dimensionality(), ax1, ax2));
  }

  /**
   * Constructor with a given database and axes.
   * 
   * @param db Database
   * @param scales Scales to use
   * @param proj Projection to use
   */
  public VisualizationProjection(Database<? extends NumberVector<?,?>> db, LinearScale[] scales, AffineTransformation proj) {
    if(scales == null) {
      scales = Scales.calcScales(db);
    }
    if(proj == null) {
      proj = AffineTransformation.reorderAxesTransformation(dim, new int[] { 1, 2 });
    }

    this.dim = db.dimensionality();
    this.scales = scales;
    this.proj = proj;
  }

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public Vector projectScaledToRender(Vector v) {
    return proj.apply(v);
  }

  /**
   * Project a vector from rendering space to scaled space.
   * 
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  public Vector projectRenderToScaled(Vector v) {
    return proj.applyInverse(v);
  }

  /**
   * Project a relative vector from scaled space to rendering space.
   * 
   * @param v relative vector in scaled space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeScaledToRender(Vector v) {
    return proj.applyRelative(v);
  }

  /**
   * Project a relative vector from rendering space to scaled space.
   * 
   * @param v relative vector in rendering space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeRenderToScaled(Vector v) {
    return proj.applyRelativeInverse(v);
  }

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(NumberVector<?,?> data) {
    Vector vec = new Vector(dim);
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getScaled(data.doubleValue(d)));
    }
    return vec;
  }

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(double[] data) {
    Vector vec = new Vector(dim);
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getScaled(data[d - 1]));
    }
    return vec;
  }

  /**
   * Project a vector from scaled space to data space.
   * 
   * @param v vector in scaled space
   * @param sampleobject Sample object needed for instantiation via
   *        {@link de.lmu.ifi.dbs.elki.data.NumberVector#newInstance}
   * @return vector in data space
   */
  public <NV extends NumberVector<NV,?>> NV projectScaledToDataSpace(Vector v, NV sampleobject) {
    Vector vec = v.copy();
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getUnscaled(vec.get(d - 1)));
    }
    return sampleobject.newInstance(vec);
  }

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(NumberVector<?,?> data) {
    Vector vec = new Vector(dim);
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getRelativeScaled(data.doubleValue(d)));
    }
    return vec;
  }

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(double[] data) {
    Vector vec = new Vector(dim);
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getRelativeScaled(data[d - 1]));
    }
    return vec;
  }

  /**
   * Project a relative vector from scaled space to data space.
   * 
   * @param v relative vector in scaled space
   * @param sampleobject Sample object needed for instantiation via
   *        {@link de.lmu.ifi.dbs.elki.data.NumberVector#newInstance}
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV,?>> NV projectRelativeScaledToDataSpace(Vector v, NV sampleobject) {
    Vector vec = v.copy();
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getRelativeUnscaled(vec.get(d - 1)));
    }
    return sampleobject.newInstance(vec);
  }

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(NumberVector<?,?> data) {
    Vector vec = projectDataToScaledSpace(data);
    return projectScaledToRender(vec);
  }

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(double[] data) {
    Vector vec = projectDataToScaledSpace(data);
    return projectScaledToRender(vec);
  }

  /**
   * Project a vector from rendering space to data space.
   * 
   * @param v vector in rendering space
   * @param sampleobject Sample object needed for instantiation via
   *        {@link de.lmu.ifi.dbs.elki.data.NumberVector#newInstance}
   * @return vector in data space
   */
  public <NV extends NumberVector<NV,?>> NV projectRenderToDataSpace(Vector v, NV sampleobject) {
    Vector vec = projectRenderToScaled(v);
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getUnscaled(vec.get(d - 1)));
    }
    return sampleobject.newInstance(vec);
  }

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(NumberVector<?,?> data) {
    Vector vec = projectRelativeDataToScaledSpace(data);
    return projectRelativeScaledToRender(vec);
  }

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(double[] data) {
    Vector vec = projectRelativeDataToScaledSpace(data);
    return projectRelativeScaledToRender(vec);
  }

  /**
   * Project a relative vector from rendering space to data space.
   * 
   * @param v relative vector in rendering space
   * @param sampleobject Sample object needed for instantiation via
   *        {@link de.lmu.ifi.dbs.elki.data.NumberVector#newInstance}
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV,?>> NV projectRelativeRenderToDataSpace(Vector v, NV sampleobject) {
    Vector vec = projectRelativeRenderToScaled(v);
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 1; d <= dim; d++) {
      vec.set(d - 1, scales[d].getRelativeUnscaled(vec.get(d - 1)));
    }
    return sampleobject.newInstance(vec);
  }

  /**
   * Get the scales used, for rendering scales mostly.
   * 
   * @param d Dimension
   * @return Scale used
   */
  public LinearScale getScale(int d) {
    return scales[d];
  }

  /**
   * Estimate the viewport requirements
   * 
   * @return Minimum and Maximum values obtained from projecting scale endpoints
   */
  public MinMax<Double>[] estimateViewport() {
    // setup the MinMax array - ugly, but nicer syntax below then...
    Class<MinMax<Double>> minmaxc = ClassGenericsUtil.uglyCastIntoSubclass(MinMax.class);
    MinMax<Double>[] minmax = ClassGenericsUtil.newArrayOfNull(2, minmaxc);
    minmax[0] = new MinMax<Double>();
    minmax[1] = new MinMax<Double>();

    // 1. Make sure the origin and the diagonal point opposite to the origin are
    // in.
    Vector orig = new Vector(dim);
    Vector diag = new Vector(dim);
    for(int d2 = 0; d2 < dim; d2++) {
      diag.set(d2, 1);
    }
    orig = projectScaledToRender(orig);
    diag = projectScaledToRender(diag);
    for(int i = 0; i <= 1; i++) {
      minmax[i].put(orig.get(i));
      minmax[i].put(diag.get(i));
    }
    // 2. Compute axis endpoints, they should be in, too
    for(int d = 1; d <= dim; d++) {
      Vector v = new Vector(dim);
      v.set(d - 1, 1);
      Vector ax = projectScaledToRender(v);
      for(int i = 0; i <= 1; i++) {
        minmax[i].put(ax.get(i));
      }
    }
    return minmax;
  }

  /**
   * Get a SVG transformation string to bring the contents into the unit cube.
   * 
   * @param margin extra margin to add.
   * @return transformation string.
   */
  public String estimateTransformString(double margin, double width, double height) {
    MinMax<Double>[] minmax = estimateViewport();
    // auto sizing magic, especially for rotated plots.
    double sizex = (minmax[0].getMax() - minmax[0].getMin()) / width;
    double sizey = (minmax[1].getMax() - minmax[1].getMin()) / height;
    double sizem = Math.max(sizex, sizey);
    double offx = (sizex - sizem) / 2 - margin;
    double offy = (sizey - sizem) / 2 - margin;
    double scale = 1./(sizem + 2 * margin);
    String left = FormatUtil.NF4.format(-(minmax[0].getMin() + offx));
    String top = FormatUtil.NF4.format(-(minmax[0].getMin() + offy));
    return "scale("+FormatUtil.NF4.format(scale)+") translate("+left+" "+top+")";
  }
  
  /**
   * Compute an transformation matrix to show only axis ax1 and ax2.
   * 
   * @param dim Dimensionality
   * @param ax1 First axis
   * @param ax2 Second axis
   * @return transformation matrix
   */
  public static AffineTransformation axisProjection(int dim, int ax1, int ax2) {
    // setup a projection to get the data into the interval -1:+1 in each
    // dimension with the intended-to-see dimensions first.
    AffineTransformation proj = AffineTransformation.reorderAxesTransformation(dim, new int[] { ax1, ax2 });
    // Assuming that the data was normalized on [0:1], center it:
    double[] trans = new double[dim];
    for(int i = 0; i < dim; i++) {
      trans[i] = -.5;
    }
    proj.addTranslation(new Vector(trans));
    // we need to mirror the y axis, since the SVG coordinate system is upside
    // down.
    proj.addAxisReflection(2);
    // scale it up
    proj.addScaling(2.0);

    return proj;
  }
}
