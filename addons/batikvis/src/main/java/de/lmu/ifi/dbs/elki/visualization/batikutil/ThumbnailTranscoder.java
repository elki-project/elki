/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * Transcode images to in-memory thumbnails.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ThumbnailTranscoder extends ImageTranscoder {
  /**
   * Last image produced.
   */
  private BufferedImage lastimg;

  /**
   * Constructor.
   */
  public ThumbnailTranscoder() {
    super();
    hints.put(KEY_FORCE_TRANSPARENT_WHITE, Boolean.FALSE);
  }

  @Override
  public BufferedImage createImage(int width, int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
  }

  /**
   * Output will be ignored!
   */
  @Override
  public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
    lastimg = img;
  }

  /**
   * Get the latest image produced.
   * 
   * @return the last image produced
   */
  public BufferedImage getLastImage() {
    return lastimg;
  }
}