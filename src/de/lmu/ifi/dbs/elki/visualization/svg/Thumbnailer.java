package de.lmu.ifi.dbs.elki.visualization.svg;

import java.io.File;
import java.io.IOException;

import org.apache.batik.transcoder.TranscoderException;

/**
 * Class that will render a {@link SVGPlot} into a {@link File} as thumbnail.
 * 
 * Note: this does not happen in the background - call it from your own Thread if you need that!
 * 
 * @author Erich Schubert
 */
public class Thumbnailer {
  /**
   * Default prefix
   */
  private static final String DEFAULT_PREFIX = "elki-";
  
  /**
   * Prefix storage.
   */
  private String prefix;
  
  /**
   * Constructor
   * @param prefix Filename prefix to avoid collisions (e.g "elki-")
   */
  public Thumbnailer(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Constructor
   */
  public Thumbnailer() {
    this(DEFAULT_PREFIX);
  }

  /**
   * Generate a thumbnail for a given plot.
   * 
   * @param plot Plot to use
   * @param thumbnailsize Size of the thumbnail
   * @return File object of the thumbnail, which has deleteOnExit set.
   */
  public synchronized File thumbnail(SVGPlot plot, int thumbnailsize) {
    File temp = null;
    try {
      temp = File.createTempFile(prefix, ".png");
      temp.deleteOnExit();
      plot.saveAsPNG(temp, thumbnailsize, thumbnailsize);
    }
    catch(TranscoderException e) {
      e.printStackTrace();
    }
    catch(IOException e) {
      e.printStackTrace();
    }
    return temp;
  }
}