package experimentalcode.erich.visualization;

import java.io.File;
import java.io.IOException;

import org.apache.batik.transcoder.TranscoderException;

/**
 * Class that will render a {@link SVGPlot} into a {@link File} as thumbnail. 
 * 
 * @author Erich Schubert
 *
 */
class Thumbnailer {
  /**
   * Constructor
   */
  public Thumbnailer() {
    // Nothing to do.
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
      temp = File.createTempFile("elki-viz-", ".png");
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