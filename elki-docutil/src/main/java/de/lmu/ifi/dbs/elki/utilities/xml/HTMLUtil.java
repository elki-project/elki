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
package de.lmu.ifi.dbs.elki.utilities.xml;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class with HTML related utility functions, in particular HTML generation.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class HTMLUtil {
  /**
   * Private constructor. Static methods only.
   */
  private HTMLUtil() {
    // Do not use.
  }

  /**
   * HTML namespace
   */
  public static final String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

  /**
   * XHTML PUBLIC doctype
   */
  public static final String HTML_XHTML_TRANSITIONAL_DOCTYPE_PUBLIC = "-//W3C//DTD XHTML 1.0 Transitional//EN";

  /**
   * XHTML SYSTEM doctype
   */
  public static final String HTML_XHTML_TRANSITIONAL_DOCTYPE_SYSTEM = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";

  /**
   * HTML root element
   */
  public static final String HTML_HTML_TAG = "html";

  /**
   * HTML head element
   */
  public static final String HTML_HEAD_TAG = "head";

  /**
   * HTML title element
   */
  public static final String HTML_TITLE_TAG = "title";

  /**
   * HTML body element
   */
  public static final String HTML_BODY_TAG = "body";

  /**
   * HTML dl element
   */
  public static final String HTML_DL_TAG = "dl";

  /**
   * HTML dt element
   */
  public static final String HTML_DT_TAG = "dt";

  /**
   * HTML dd element
   */
  public static final String HTML_DD_TAG = "dd";

  /**
   * HTML unordered list tag
   */
  public static final String HTML_UL_TAG = "ul";

  /**
   * HTML ordered list tag
   */
  public static final String HTML_OL_TAG = "ol";

  /**
   * HTML list item tag
   */
  public static final String HTML_LI_TAG = "li";

  /**
   * HTML em element
   */
  public static final String HTML_EM_TAG = "em";

  /**
   * HTML i element
   */
  public static final String HTML_I_TAG = "i";

  /**
   * HTML strong element
   */
  public static final String HTML_STRONG_TAG = "strong";

  /**
   * HTML b element
   */
  public static final String HTML_B_TAG = "b";

  /**
   * HTML tt element
   */
  public static final String HTML_TT_TAG = "tt";

  /**
   * HTML br element
   */
  public static final String HTML_BR_TAG = "br";

  /**
   * HTML h1 element
   */
  public static final String HTML_H1_TAG = "h1";

  /**
   * HTML a element
   */
  public static final String HTML_A_TAG = "a";

  /**
   * HTML p element
   */
  public static final String HTML_P_TAG = "p";

  /**
   * HTML div element
   */
  public static final String HTML_DIV_TAG = "div";

  /**
   * HTML span element
   */
  public static final String HTML_SPAN_TAG = "span";

  /**
   * HTML img element
   */
  public static final String HTML_IMG_TAG = "img";

  /**
   * HTML meta element
   */
  public static final String HTML_META_TAG = "meta";

  /**
   * HTML link element
   */
  public static final String HTML_LINK_TAG = "link";

  /**
   * HTML href attribute (a, link tags)
   */
  public static final String HTML_HREF_ATTRIBUTE = "href";

  /**
   * HTML src attribute (img tag)
   */
  public static final String HTML_SRC_ATTRIBUTE = "src";

  /**
   * HTML style attribute
   */
  public static final String HTML_STYLE_ATTRIBUTE = "style";

  /**
   * HTML class attribute
   */
  public static final String HTML_CLASS_ATTRIBUTE = "class";

  /**
   * HTML element id attribute
   */
  public static final String HTML_ID_ATTRIBUTE = "id";

  /**
   * HTML name attribute (e.g. A tag)
   */
  public static final String HTML_NAME_ATTRIBUTE = "name";

  /**
   * HTML type attribute (link tag)
   */
  public static final String HTML_TYPE_ATTRIBUTE = "type";

  /**
   * HTML rel attribute (link tag)
   */
  public static final String HTML_REL_ATTRIBUTE = "rel";

  /**
   * HTML rel value for stylesheets
   */
  public static final String HTML_REL_STYLESHEET = "stylesheet";

  /**
   * HTML http-equiv attribute (meta tag)
   */
  public static final String HTML_HTTP_EQUIV_ATTRIBUTE = "http-equiv";

  /**
   * HTML content attribute (meta tag)
   */
  public static final String HTML_CONTENT_ATTRIBUTE = "content";

  /**
   * HTML http-equiv value Content-type
   */
  public static final String HTML_HTTP_EQUIV_CONTENT_TYPE = "Content-Type";

  /**
   * HTML content type
   */
  public static final String CONTENT_TYPE_HTML = "text/html";

  /**
   * CSS content type
   */
  public static final String CONTENT_TYPE_CSS = "text/css";

  /**
   * HTML content type with UTF-8 indication
   */
  public static final String CONTENT_TYPE_HTML_UTF8 = CONTENT_TYPE_HTML + "; charset=UTF-8";

  /**
   * Write an HTML document to an output stream.
   * 
   * @param htmldoc Document to output
   * @param out Stream to write to
   * @throws IOException thrown on IO errors
   */
  public static void writeXHTML(Document htmldoc, OutputStream out) throws IOException {
    javax.xml.transform.Result result = new StreamResult(out);
    // Use a transformer for pretty printing
    Transformer xformer;
    try {
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");
      // TODO: ensure the "meta" tag doesn't claim a different encoding!
      xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      xformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, HTML_XHTML_TRANSITIONAL_DOCTYPE_PUBLIC);
      xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, HTML_XHTML_TRANSITIONAL_DOCTYPE_SYSTEM);
      xformer.transform(new DOMSource(htmldoc), result);
    }
    catch(TransformerException e1) {
      throw new IOException(e1);
    }
    out.flush();
  }

  /**
   * Append a multiline text to a node, transforming linewraps into BR tags.
   * 
   * @param htmldoc Document
   * @param parent Parent node
   * @param text Text to add.
   * @return parent node
   */
  public static Element appendMultilineText(Document htmldoc, Element parent, String text) {
    String[] parts = text != null ? text.split("\n") : null;
    if(parts == null || parts.length == 0) {
      return parent;
    }
    parent.appendChild(htmldoc.createTextNode(parts[0]));
    for(int i = 1; i < parts.length; i++) {
      parent.appendChild(htmldoc.createElement(HTML_BR_TAG));
      parent.appendChild(htmldoc.createTextNode(parts[i]));
    }
    return parent;
  }
}
