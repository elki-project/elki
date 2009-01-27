package experimentalcode.erich.visualization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

/**
 * Class that will render a {@link SVGPlot} into a {@link File} as thumbnail. 
 * 
 * @author Erich Schubert
 *
 */
class Thumbnailer {
  /**
   * The actual transcoder to use
   */
  private PNGTranscoder t;

  /**
   * Constructor
   */
  public Thumbnailer() {
    t = new PNGTranscoder();
  }

  /**
   * Generate a thumbnail for a given plot.
   * 
   * @param plot Plot to use
   * @param thumbnailsize Size of the thumbnail
   * @return File object of the thumbnail, which has deleteOnExit set.
   */
  public synchronized File thumbnail(SVGPlot plot, int thumbnailsize) {
    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(thumbnailsize));
    t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(thumbnailsize));
    File temp = null;
    try {
      temp = File.createTempFile("elki-viz-", ".png");
      temp.deleteOnExit();
      TranscoderInput input = new TranscoderInput(plot.getDocument());
      OutputStream obuf = new FileOutputStream(temp);
      TranscoderOutput output = new TranscoderOutput(obuf);
      t.transcode(input, output);
      obuf.flush();
      obuf.close();
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