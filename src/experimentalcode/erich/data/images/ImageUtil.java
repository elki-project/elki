package experimentalcode.erich.data.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Class with generic image handling utility functions.
 * 
 * @author Erich Schubert
 */
public final class ImageUtil {
  /**
   * Load an image from a file using ImageIO.
   * 
   * @param file File name
   * @return Image
   * @throws IOException
   */
  public static BufferedImage loadImage(File file) throws IOException {
    ImageInputStream is = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(is);
  
    if(!iter.hasNext()) {
      throw new IOException("Unsupported file format.");
    }
    ImageReader imageReader = iter.next();
    imageReader.setInput(is);
    return imageReader.read(0);
  }
}
