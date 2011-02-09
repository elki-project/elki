package de.lmu.ifi.dbs.elki.result;

import java.awt.image.RenderedImage;
import java.io.File;

/**
 * Result encapsulating a single image.
 * 
 * @author Erich Schubert
 */
public interface PixmapResult extends Result {
  /**
   * Get the image pixmap
   * 
   * @return the image pixmap
   */
  public RenderedImage getImage();
  
  /**
   * Get the image result as file (usually a temporary file).
   * 
   * @return Image file
   */
  public File getAsFile();
}