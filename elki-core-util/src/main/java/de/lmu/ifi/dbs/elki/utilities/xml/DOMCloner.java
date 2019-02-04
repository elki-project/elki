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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Class for cloning XML document, with filter capabilites
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class DOMCloner {
  /**
   * Deep-clone a document.
   * 
   * @param domImpl DOM implementation to use
   * @param document Original document
   * @return Cloned document
   */
  public Document cloneDocument(DOMImplementation domImpl, Document document) {
    Element root = document.getDocumentElement();
    // New document
    Document result = domImpl.createDocument(root.getNamespaceURI(), root.getNodeName(), null);
    Element rroot = result.getDocumentElement();
    // Cloning the document element is a bit tricky.
    // This is adopted from DomUtilities#deepCloneDocument
    boolean before = true;
    for(Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
      if(n == root) {
        before = false;
        copyAttributes(result, root, rroot);
        for(Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
          final Node cl = cloneNode(result, c);
          if(cl != null) {
            rroot.appendChild(cl);
          }
        }
      }
      else {
        if(n.getNodeType() != Node.DOCUMENT_TYPE_NODE) {
          final Node cl = cloneNode(result, n);
          if(cl != null) {
            if(before) {
              result.insertBefore(cl, rroot);
            }
            else {
              result.appendChild(cl);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Clone an existing node.
   * 
   * @param doc Document
   * @param eold Existing node
   * @return Cloned node
   */
  public Node cloneNode(Document doc, Node eold) {
    return doc.importNode(eold, true);
  }

  /**
   * Copy the attributes from an existing node to a new node.
   * 
   * @param doc Target document
   * @param eold Existing node
   * @param enew Target node
   */
  public void copyAttributes(Document doc, Element eold, Element enew) {
    if(eold.hasAttributes()) {
      NamedNodeMap attr = eold.getAttributes();
      int len = attr.getLength();
      for(int i = 0; i < len; i++) {
        enew.setAttributeNode((Attr) doc.importNode(attr.item(i), true));
      }
    }
  }
}