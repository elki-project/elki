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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Iterator;

import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.RedRable;
import org.apache.batik.ext.awt.image.spi.AbstractRegistryEntry;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.spi.MagicNumberRegistryEntry;
import org.apache.batik.ext.awt.image.spi.URLRegistryEntry;
import org.apache.batik.svggen.ErrorConstants;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.ParsedURLData;
import org.apache.batik.util.ParsedURLProtocolHandler;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Access images via an internal image registry.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ThumbnailRegistryEntry extends AbstractRegistryEntry implements URLRegistryEntry, ParsedURLProtocolHandler {
  /**
   * ELKI internal thumbnail protocol id.
   */
  public static final String INTERNAL_PROTOCOL = "thumb";

  /**
   * ELKI internal thumbnail protocol prefix
   */
  public static final String INTERNAL_PREFIX = INTERNAL_PROTOCOL + ":";

  /**
   * Mime type
   */
  public static final String INTERNAL_MIME_TYPE = "internal/thumb";

  /**
   * The priority of this entry.
   */
  public static final float PRIORITY = 1 * MagicNumberRegistryEntry.PRIORITY;

  /**
   * The logger class.
   */
  private static final Logging LOG = Logging.getLogger(ThumbnailRegistryEntry.class);

  /**
   * The image cache.
   */
  private static final Int2ObjectOpenHashMap<SoftReference<RenderedImage>> images = new Int2ObjectOpenHashMap<>();

  /**
   * Object counter
   */
  private static int counter = 1;

  /**
   * Constructor.
   *
   * Note: there will usually be two instances created. One for handling the
   * image type, one for the URL handling. This is ok.
   */
  public ThumbnailRegistryEntry() {
    super("Internal", PRIORITY, new String[0], new String[] { INTERNAL_MIME_TYPE });
    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner("Registry initialized.");
    }
  }

  /**
   * Put an image into the repository (note: the repository is only keeping a
   * weak reference!)
   *
   * @param img Image to put
   * @return Key
   */
  public static int registerImage(RenderedImage img) {
    synchronized(images) {
      int key = counter;
      counter++;
      assert (images.get(key) == null);
      images.put(key, new SoftReference<>(img));
      // Reorganize map, purge old entries
      if(counter % 50 == 49) {
        for(Iterator<SoftReference<RenderedImage>> iter = images.values().iterator(); iter.hasNext();) {
          SoftReference<RenderedImage> ref = iter.next();
          if(ref == null || ref.get() == null) {
            iter.remove();
          }
        }
      }
      if(LOG.isDebuggingFiner()) {
        LOG.debugFiner("Registered image: " + key);
      }
      return key;
    }
  }

  @Override
  public boolean isCompatibleURL(ParsedURL url) {
    // logger.warning("isCompatible " + url.toString());
    return isCompatibleURLStatic(url);
  }

  /**
   * Test for a compatible URL.
   *
   * @param url URL
   * @return Success code
   */
  public static boolean isCompatibleURLStatic(ParsedURL url) {
    return url.getProtocol().equals(INTERNAL_PROTOCOL);
  }

  @Override
  public Filter handleURL(ParsedURL url, boolean needRawData) {
    Filter ret = handleURL(url);
    if(ret != null) {
      return ret;
    }
    // Image not found in registry.
    return ImageTagRegistry.getBrokenLinkImage(this, ErrorConstants.ERR_IMAGE_DIR_DOES_NOT_EXIST, new Object[0]);
  }

  /**
   * Statically handle the URL access.
   *
   * @param url URL to access
   * @return Image, or null
   */
  public static Filter handleURL(ParsedURL url) {
    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner("handleURL " + url.toString());
    }
    if(!isCompatibleURLStatic(url)) {
      return null;
    }
    int id;
    try {
      id = ParseUtil.parseIntBase10(url.getPath());
    }
    catch(NumberFormatException e) {
      return null;
    }
    SoftReference<RenderedImage> ref = images.get(id);
    if(ref != null) {
      RenderedImage ri = ref.get();
      if(ri == null) {
        LOG.warning("Referenced image has expired from the cache!");
      }
      else {
        return new RedRable(GraphicsUtil.wrap(ri));
      }
    }
    // Image not found in registry.
    return null;
  }

  /**
   * URL representation for internal URLs.
   *
   * @author Erich Schubert
   */
  private static class InternalParsedURLData extends ParsedURLData {
    /**
     * Constructor.
     */
    public InternalParsedURLData(String id) {
      super();
      this.protocol = INTERNAL_PROTOCOL;
      this.contentType = INTERNAL_MIME_TYPE;
      this.path = id;
    }

    @Override
    public String getContentType(String userAgent) {
      return INTERNAL_MIME_TYPE;
    }

    @Override
    public boolean complete() {
      return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public InputStream openStream(String userAgent, Iterator mimeTypes) throws IOException {
      // Return null, since we don't want to use streams.
      return null;
    }
  }

  @Override
  public ParsedURLData parseURL(String urlStr) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("parseURL: " + urlStr);
    }
    if(urlStr.startsWith(INTERNAL_PREFIX)) {
      InternalParsedURLData ret = new InternalParsedURLData(urlStr.substring(INTERNAL_PREFIX.length()));
      return ret;
    }
    return null;
  }

  @Override
  public ParsedURLData parseURL(ParsedURL basepurl, String urlStr) {
    // Won't happen in a relative way anyway, and is not particularly
    // supported (as the objects might be dropped from the cache)
    return parseURL(urlStr);
  }

  @Override
  public String getProtocolHandled() {
    return INTERNAL_PROTOCOL;
  }
}