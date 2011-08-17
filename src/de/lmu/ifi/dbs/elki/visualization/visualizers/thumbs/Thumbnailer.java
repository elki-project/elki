package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;
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

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Class that will render a {@link SVGPlot} into a {@link File} as thumbnail.
 * 
 * Note: this does not happen in the background - call it from your own Thread if you need that!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses SVGPlot oneway - - renders
 * @apiviz.has File oneway - - «create» 
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
   * @param thumbwidth Width of the thumbnail
   * @param thumbheight height of the thumbnail
   * @return File object of the thumbnail, which has deleteOnExit set.
   */
  public synchronized File thumbnail(SVGPlot plot, int thumbwidth, int thumbheight) {
    File temp = null;
    try {
      temp = File.createTempFile(prefix, ".png");
      temp.deleteOnExit();
      plot.saveAsPNG(temp, thumbwidth, thumbheight);
    }
    catch(org.apache.batik.bridge.BridgeException e) {
      plot.dumpDebugFile();
      LoggingUtil.exception("Exception rendering thumbnail: ", e);
    }
    catch(org.apache.batik.transcoder.TranscoderException e) {
      plot.dumpDebugFile();
      LoggingUtil.exception("Exception rendering thumbnail: ", e);
    }
    catch(IOException e) {
      LoggingUtil.exception(e);
    }
    return temp;
  }
}