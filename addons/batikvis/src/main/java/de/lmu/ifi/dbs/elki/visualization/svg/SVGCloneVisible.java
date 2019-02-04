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
package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.lmu.ifi.dbs.elki.utilities.xml.DOMCloner;

/**
 * Clone visible parts of an SVG document.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class SVGCloneVisible extends DOMCloner {
  @Override
  public Node cloneNode(Document doc, Node eold) {
    // Skip elements with visibility=hidden
    if(eold instanceof Element) {
      Element eeold = (Element) eold;
      String vis = eeold.getAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
      if(SVGConstants.CSS_HIDDEN_VALUE.equals(vis)) {
        return null;
      }
    }
    // Perform clone flat
    Node enew = doc.importNode(eold, false);
    // Recurse:
    for(Node n = eold.getFirstChild(); n != null; n = n.getNextSibling()) {
      final Node clone = cloneNode(doc, n);
      if (clone != null) {
        enew.appendChild(clone);
      }
    }
    return enew;
  }
}
