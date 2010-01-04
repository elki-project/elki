package de.lmu.ifi.dbs.elki.data.images;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Class to use in {@link java.awt.Graphics2D#setComposite} that facilitates
 * basic image blending such as lighten-only overlays.
 * 
 * @author Erich Schubert
 */
public class BlendComposite implements Composite {
  /**
   * Source blending, using the source image only.
   */
  public static final int NORMAL = 0;

  /**
   * Lighten-only blending.
   */
  public static final int LIGHTEN = 1;

  /**
   * Darken-only blending.
   */
  public static final int DARKEN = 2;

  /**
   * "Screen" blending.
   */
  public static final int SCREEN = 3;

  /**
   * "Multiply" blending.
   */
  public static final int MULTIPLY = 4;

  /**
   * "Overlay" blending.
   */
  public static final int OVERLAY = 5;

  /**
   * "Average" blending.
   */
  public static final int AVERAGE = 6;
  
  // TODO: Add the missing common blend modes:
  // ADD, Color Burn, Color Dodge, Reflect, Glow, Difference, Negation

  /**
   * Alpha (opacity) value.
   */
  private double alpha;

  /**
   * Blending mode to use.
   */
  private int mode;

  /**
   * Simplified constructor with full opacity.
   * 
   * @param mode Blending mode.
   */
  public BlendComposite(int mode) {
    this(mode, 1.0);
  }

  /**
   * Full constructor, with alpha (opacity) value.
   * 
   * @param mode Blending mode
   * @param alpha Opacity value
   */
  public BlendComposite(int mode, double alpha) {
    this.mode = mode;
    this.alpha = alpha;
  }

  @Override
  public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, @SuppressWarnings("unused") RenderingHints hints) {
    switch(mode){
    case NORMAL:
      return new BlendingContext(srcColorModel, dstColorModel, alpha);
    case LIGHTEN:
      return new LightenBlendingContext(srcColorModel, dstColorModel, alpha);
    case DARKEN:
      return new DarkenBlendingContext(srcColorModel, dstColorModel, alpha);
    case SCREEN:
      return new ScreenBlendingContext(srcColorModel, dstColorModel, alpha);
    case MULTIPLY:
      return new MultiplyBlendingContext(srcColorModel, dstColorModel, alpha);
    case OVERLAY:
      return new OverlayBlendingContext(srcColorModel, dstColorModel, alpha);
    case AVERAGE:
      return new AverageBlendingContext(srcColorModel, dstColorModel, alpha);
    default:
      return new BlendingContext(srcColorModel, dstColorModel, alpha);
    }
  }

  /**
   * Abstract blending context that takes care of color space conversion and
   * pixel iteration. The base class does simple replacing.
   * 
   * @author Erich Schubert
   */
  protected class BlendingContext implements CompositeContext {
    /**
     * Source color model
     */
    private ColorModel srcColorModel;

    /**
     * Destination color model
     */
    private ColorModel dstColorModel;

    /**
     * Opacity factor
     */
    protected double alpha;

    /**
     * Additive inverse of alpha value.
     */
    protected double ialpha;

    /**
     * Constructor.
     * 
     * @param srcColorModel source color model
     * @param dstColorModel destination color model
     * @param alpha Alpha (opacity) factor
     */
    protected BlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      this.srcColorModel = srcColorModel;
      this.dstColorModel = dstColorModel;
      this.alpha = alpha;
      this.ialpha = 1. - alpha;
    }

    @Override
    public void dispose() {
      // Nothing to do by default
    }

    /**
     * Compose a raster image (source) and a background (destination) to a
     * result raster.
     */
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
      int width = Math.min(src.getWidth(), dstIn.getWidth());
      int height = Math.min(src.getHeight(), dstIn.getHeight());

      Object resPixel = null;
      Object srcPixel = null;
      Object dstPixel = null;

      for(int y = dstOut.getMinY(); y < height; y++) {
        for(int x = dstOut.getMinX(); x < width; x++) {
          srcPixel = src.getDataElements(x, y, srcPixel);
          dstPixel = dstIn.getDataElements(x, y, dstPixel);

          final int srcAlpha = srcColorModel.getAlpha(srcPixel);
          final int dstAlpha = dstColorModel.getAlpha(dstPixel);
          final int srcRed = srcColorModel.getRed(srcPixel);
          final int dstRed = dstColorModel.getRed(dstPixel);
          final int srcGreen = srcColorModel.getGreen(srcPixel);
          final int dstGreen = dstColorModel.getGreen(dstPixel);
          final int srcBlue = srcColorModel.getBlue(srcPixel);
          final int dstBlue = dstColorModel.getBlue(dstPixel);
          int rgb = blend(srcAlpha, srcRed, srcGreen, srcBlue, dstAlpha, dstRed, dstGreen, dstBlue);

          resPixel = dstColorModel.getDataElements(rgb, resPixel);

          dstOut.setDataElements(x, y, resPixel);
        }
      }
    }

    /**
     * The actual blending function for two colors.
     * 
     * @param sA source alpha component (0-255)
     * @param sR source red component (0-255)
     * @param sG source green component (0-255)
     * @param sB source blue component (0-255)
     * @param dA destination alpha component (0-255)
     * @param dR destination red component (0-255)
     * @param dG destination green component (0-255)
     * @param dB destination blues component (0-255)
     * @return Combined color in single-integer ARGB format (see
     *         {@link BlendComposite#combineComponents} and {@link #mixAlpha}
     *         helper functions)
     */
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      return mixAlpha(sA, sR, sG, sB, dA, dR, dG, dB);
    }

    /**
     * Mix the new values with the original values taking the alpha value into
     * account.
     * 
     * @param nA new alpha component (0-255)
     * @param nR new red component (0-255)
     * @param nG new green component (0-255)
     * @param nB new blue component (0-255)
     * @param dA old alpha component (0-255)
     * @param dR old red component (0-255)
     * @param dG old green component (0-255)
     * @param dB old blue component (0-255)
     * @return Combined color in single-integer ARGB format.
     */
    protected int mixAlpha(final int nA, final int nR, final int nG, final int nB, final int dA, final int dR, final int dG, final int dB) {
      int a = (int) (nA * alpha + dA * ialpha);
      int r = (int) (nR * alpha + dR * ialpha);
      int g = (int) (nG * alpha + dG * ialpha);
      int b = (int) (nB * alpha + dB * ialpha);

      return combineComponents(a, r, g, b);
    }
  }

  /**
   * Helper function that combines separate ARGB values into a single ARGB
   * integer.
   * 
   * @param a alpha component (0-255)
   * @param r red component (0-255)
   * @param g green component (0-255)
   * @param b blue component (0-255)
   * @return Integer value in ARGB order.
   */
  protected static final int combineComponents(int a, int r, int g, int b) {
    return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
  }

  /**
   * Blending context for a "lighten only" blending.
   * 
   * @author Erich Schubert
   */
  class LightenBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected LightenBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = Math.max(sR, dR);
      int g = Math.max(sG, dG);
      int b = Math.max(sB, dB);

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }

  /**
   * Blending context for a "darken only" blending.
   * 
   * @author Erich Schubert
   */
  class DarkenBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected DarkenBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = Math.min(sR, dR);
      int g = Math.min(sG, dG);
      int b = Math.min(sB, dB);

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }

  /**
   * Blending context for a "screen" blending.
   * 
   * @author Erich Schubert
   */
  class ScreenBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected ScreenBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = 255 - ((255 - sR) * (255 - dR) >> 8);
      int g = 255 - ((255 - sG) * (255 - dG) >> 8);
      int b = 255 - ((255 - sB) * (255 - dB) >> 8);

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }

  /**
   * Blending context for a "multiply" blending.
   * 
   * @author Erich Schubert
   */
  class MultiplyBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected MultiplyBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = (sR * dR) >> 8;
      int g = (sG * dG) >> 8;
      int b = (sB * dB) >> 8;

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }

  /**
   * Blending context for a "overlay" blending.
   * 
   * @author Erich Schubert
   */
  class OverlayBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected OverlayBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = (dR < 128) ? (dR * sR >> 7) : (255 - ((255 - dR) * (255 - sR) >> 7));
      int g = (dG < 128) ? (dG * sG >> 7) : (255 - ((255 - dG) * (255 - sG) >> 7));
      int b = (dB < 128) ? (dB * sB >> 7) : (255 - ((255 - dB) * (255 - sB) >> 7));

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }
  
  /**
   * Blending context for an "average" blending.
   * 
   * @author Erich Schubert
   */
  class AverageBlendingContext extends BlendingContext {
    /**
     * Constructor.
     * 
     * @param srcColorModel color model
     * @param dstColorModel color model
     * @param alpha Opacity value
     */
    protected AverageBlendingContext(ColorModel srcColorModel, ColorModel dstColorModel, double alpha) {
      super(srcColorModel, dstColorModel, alpha);
    }

    @Override
    protected int blend(final int sA, final int sR, final int sG, final int sB, final int dA, final int dR, final int dG, final int dB) {
      int a = Math.min(255, sA + dA - (sA * dA) / 255);
      int r = (sR + dR) >> 1;
      int g = (sG + dG) >> 1;
      int b = (sB + dB) >> 1;

      return mixAlpha(a, r, g, b, dA, dR, dG, dB);
    }
  }
}