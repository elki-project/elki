package de.lmu.ifi.dbs.elki.result;

import java.awt.image.RenderedImage;

/**
 * Result encapsulating a single image.
 * 
 * @author Erich Schubert
 */
public interface PixmapResult extends Result {
  /**
   * @return the image
   */
  public RenderedImage getImage();
}