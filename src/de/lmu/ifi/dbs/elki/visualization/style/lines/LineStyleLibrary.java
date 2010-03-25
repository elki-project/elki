package de.lmu.ifi.dbs.elki.visualization.style.lines;

import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;

/**
 * Interface to obtain CSS classes for plot lines.
 * 
 * {@code meta} is a set of Objects, usually constants that may or may not be
 * used by the {@link LineStyleLibrary} to generate variants of the style.
 * 
 * Predefined meta flags that are usually supported are:
 * <dl>
 * <dt>{@link #FLAG_STRONG}</dt>
 * <dd>Request a "stronger" version of the same style</dd>
 * <dt>{@link #FLAG_WEAK}</dt>
 * <dd>Request a "weaker" version of the same style</dd>
 * <dt>{@link #FLAG_INTERPOLATED}</dt>
 * <dd>Request an "interpolated" version of the same style (e.g. lighter or
 * dashed)</dd>
 * </dl>
 * 
 * @author Erich Schubert
 * 
 */
public interface LineStyleLibrary {
  /**
   * Meta flag to request a 'stronger' version of the style
   */
  public final static String FLAG_STRONG = "strong";

  /**
   * Meta flag to request a 'weaker' version of the style
   */
  public final static String FLAG_WEAK = "weak";

  /**
   * Meta flag to request an 'interpolated' version of the style
   */
  public final static String FLAG_INTERPOLATED = "interpolated";

  /**
   * Add the formatting statements to the given CSS class.
   * 
   * Note: this can overwrite some existing properties of the CSS class.
   * 
   * @param cls CSS class to modify
   * @param style style number
   * @param width line width
   * @param meta meta objects to request line variants
   */
  public void formatCSSClass(CSSClass cls, int style, double width, Object... meta);
}
