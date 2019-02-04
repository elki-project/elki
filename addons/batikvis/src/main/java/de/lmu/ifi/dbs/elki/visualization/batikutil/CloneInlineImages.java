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

import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.batik.svggen.SVGSyntax;
import org.apache.batik.util.Base64EncoderStream;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGCloneVisible;

/**
 * Clone an SVG document, inlining temporary and in-memory linked images.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @has - - - ThumbnailRegistryEntry
 */
public class CloneInlineImages extends SVGCloneVisible {
  @Override
  public Node cloneNode(Document doc, Node eold) {
    Node enew = null;
    if(eold instanceof Element) {
      Element e = (Element) eold;
      if(e.getTagName().equals(SVGConstants.SVG_IMAGE_TAG)) {
        String url = e.getAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_ATTRIBUTE);
        ParsedURL urldata = new ParsedURL(url);
        if(ThumbnailRegistryEntry.isCompatibleURLStatic(urldata)) {
          enew = inlineThumbnail(doc, urldata, eold);
        }
        else if("file".equals(urldata.getProtocol())) {
          enew = inlineExternal(doc, urldata, eold);
        }
      }
    }
    if(enew != null) {
      return enew;
    }
    return super.cloneNode(doc, eold);
  }

  /**
   * Inline a referenced thumbnail.
   * 
   * @param doc Document (element factory)
   * @param urldata URL
   * @param eold Existing node
   * @return Replacement node, or {@code null}
   */
  protected Node inlineThumbnail(Document doc, ParsedURL urldata, Node eold) {
    RenderableImage img = ThumbnailRegistryEntry.handleURL(urldata);
    if(img == null) {
      LoggingUtil.warning("Image not found in registry: " + urldata.toString());
      return null;
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      os.write(SVGSyntax.DATA_PROTOCOL_PNG_PREFIX.getBytes());
      Base64EncoderStream encoder = new Base64EncoderStream(os);
      ImageIO.write(img.createDefaultRendering(), "png", encoder);
      encoder.close();
    }
    catch(IOException e) {
      LoggingUtil.exception("Exception serializing image to png", e);
      return null;
    }
    Element i = (Element) super.cloneNode(doc, eold);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_ATTRIBUTE, os.toString().replaceAll("\\s*[\\r\\n]+\\s*", ""));
    return i;
  }

  /**
   * Inline an external file (usually from temp).
   * 
   * @param doc Document (element factory)
   * @param urldata URL
   * @param eold Existing node
   * @return Replacement node, or {@code null}
   */
  protected Node inlineExternal(Document doc, ParsedURL urldata, Node eold) {
    File in = new File(urldata.getPath());
    if(!in.exists()) {
      LoggingUtil.warning("Referencing non-existant file: " + urldata.toString());
      return null;
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      os.write(SVGSyntax.DATA_PROTOCOL_PNG_PREFIX.getBytes());
      Base64EncoderStream encoder = new Base64EncoderStream(os);
      FileInputStream instream = new FileInputStream(in);
      byte[] buf = new byte[4096];
      while(true) {
        int read = instream.read(buf, 0, buf.length);
        if(read <= 0) {
          break;
        }
        encoder.write(buf, 0, read);
      }
      instream.close();
      encoder.close();
    }
    catch(IOException e) {
      LoggingUtil.exception("Exception serializing image to png", e);
      return null;
    }

    Element i = (Element) super.cloneNode(doc, eold);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_ATTRIBUTE, os.toString().replaceAll("\\s*[\\r\\n]+\\s*", ""));
    return i;
  }
}