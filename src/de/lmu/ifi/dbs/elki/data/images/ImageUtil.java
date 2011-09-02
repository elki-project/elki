package de.lmu.ifi.dbs.elki.data.images;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
 * 
 * @apiviz.has BufferedImage
 * @apiviz.uses File
 */
public final class ImageUtil {
  /**
   * Load an image from a file using ImageIO.
   * 
   * @param file File name
   * @return Image
   * @throws IOException thrown on IO errors
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
