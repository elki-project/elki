package de.lmu.ifi.dbs.elki.application.internal;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil;

/**
 * Build a reference documentation for all available parameters.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Reference
 */
public class DocumentReferences {
  private static final String CSSFILE = "stylesheet.css";

  private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(DocumentReferences.class);

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    if(args.length < 1 || args.length > 2) {
      LoggingUtil.warning("I need exactly one or two file names to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html") || (args.length > 1 && !args[1].endsWith(".wiki"))) {
      LoggingUtil.warning("File name doesn't end in expected extension!");
      System.exit(1);
    }

    List<Pair<Reference, List<Class<?>>>> refs = sortedReferences();
    try {
      File references = new File(args[0]);
      FileOutputStream reffo = new FileOutputStream(references);
      Document refdoc = documentReferences(refs);
      OutputStream refstream = new BufferedOutputStream(reffo);
      HTMLUtil.writeXHTML(refdoc, refstream);
      refstream.flush();
      refstream.close();
      reffo.close();
    }
    catch(IOException e) {
      LoggingUtil.exception("IO Exception writing HTML output.", e);
      throw new RuntimeException(e);
    }
    if(args.length > 1) {
      try {
        File refwiki = new File(args[1]);
        FileOutputStream reffow = new FileOutputStream(refwiki);
        PrintStream refstreamW = new PrintStream(reffow);
        documentReferencesWiki(refs, refstreamW);
        refstreamW.flush();
        refstreamW.close();
        reffow.close();
      }
      catch(IOException e) {
        LoggingUtil.exception("IO Exception writing Wiki output.", e);
        throw new RuntimeException(e);
      }
    }
  }

  private static Document documentReferences(List<Pair<Reference, List<Class<?>>>> refs) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch(ParserConfigurationException e1) {
      throw new RuntimeException(e1);
    }
    DOMImplementation impl = builder.getDOMImplementation();
    Document htmldoc = impl.createDocument(HTMLUtil.HTML_NAMESPACE, HTMLUtil.HTML_HTML_TAG, null);
    // head
    Element head = htmldoc.createElement(HTMLUtil.HTML_HEAD_TAG);
    htmldoc.getDocumentElement().appendChild(head);
    // body
    Element body = htmldoc.createElement(HTMLUtil.HTML_BODY_TAG);
    htmldoc.getDocumentElement().appendChild(body);
    // modification warnings
    {
      Comment warn = htmldoc.createComment(MODIFICATION_WARNING);
      head.appendChild(warn);
      Comment warn2 = htmldoc.createComment(MODIFICATION_WARNING);
      body.appendChild(warn2);
    }
    // meta with charset information
    {
      Element meta = htmldoc.createElement(HTMLUtil.HTML_META_TAG);
      meta.setAttribute(HTMLUtil.HTML_HTTP_EQUIV_ATTRIBUTE, HTMLUtil.HTML_HTTP_EQUIV_CONTENT_TYPE);
      meta.setAttribute(HTMLUtil.HTML_CONTENT_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_HTML_UTF8);
      head.appendChild(meta);
    }
    // stylesheet
    {
      Element css = htmldoc.createElement(HTMLUtil.HTML_LINK_TAG);
      css.setAttribute(HTMLUtil.HTML_REL_ATTRIBUTE, HTMLUtil.HTML_REL_STYLESHEET);
      css.setAttribute(HTMLUtil.HTML_TYPE_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_CSS);
      css.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, CSSFILE);
      head.appendChild(css);
    }
    // title
    {
      Element title = htmldoc.createElement(HTMLUtil.HTML_TITLE_TAG);
      title.setTextContent("ELKI references overview.");
      head.appendChild(title);
    }
    // Heading
    {
      Element h1 = htmldoc.createElement(HTMLUtil.HTML_H1_TAG);
      h1.setTextContent("ELKI references overview:");
      body.appendChild(h1);
    }

    // Main definition list
    Element maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
    body.appendChild(maindl);
    for(Pair<Reference, List<Class<?>>> pair : refs) {
      // DT = definition term
      Element classdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        boolean first = true;
        for(Class<?> cls : pair.second) {
          if(!first) {
            classdt.appendChild(htmldoc.createTextNode(", "));
          }
          Element classan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          classan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, cls.getName());
          classdt.appendChild(classan);

          // Link back to original class
          Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(cls.getName()));
          classa.setTextContent(cls.getName());
          classdt.appendChild(classa);

          first = false;
        }
      }
      maindl.appendChild(classdt);
      // DD = definition description
      Element classdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      maindl.appendChild(classdd);

      {
        Reference ref = pair.first;
        // Prefix
        if(ref.prefix().length() > 0) {
          Element prediv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
          prediv.setTextContent(ref.prefix());
          classdd.appendChild(prediv);
        }
        // Authors
        Element authorsdiv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
        authorsdiv.setTextContent(ref.authors());
        classdd.appendChild(authorsdiv);
        // Title
        Element titlediv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
        Element titleb = htmldoc.createElement(HTMLUtil.HTML_B_TAG);
        titleb.setTextContent(ref.title());
        titlediv.appendChild(titleb);
        classdd.appendChild(titlediv);
        // Booktitle
        Element booktitlediv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
        booktitlediv.setTextContent("In: " + ref.booktitle());
        classdd.appendChild(booktitlediv);
        // URL
        if(ref.url().length() > 0) {
          Element urldiv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
          Element urla = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          urla.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, ref.url());
          urla.setTextContent(ref.url());
          urldiv.appendChild(urla);
          classdd.appendChild(urldiv);
        }
      }
    }
    return htmldoc;
  }

  private static void documentReferencesWiki(List<Pair<Reference, List<Class<?>>>> refs, PrintStream refstreamW) {
    for(Pair<Reference, List<Class<?>>> pair : refs) {
      // JavaDoc links for relevant classes.
      {
        boolean first = true;
        for(Class<?> cls : pair.second) {
          if(!first) {
            refstreamW.println(",[[br]]");
          }
          refstreamW.print("[[javadoc(");
          refstreamW.print(cls.getName());
          refstreamW.print(",");
          refstreamW.print(cls.getName());
          refstreamW.print(")]]");

          first = false;
        }
      }
      refstreamW.println("");

      String indent = " ";
      {
        Reference ref = pair.first;
        // Prefix
        if(ref.prefix().length() > 0) {
          refstreamW.println(indent + ref.prefix() + " [[br]]");
        }
        // Authors
        refstreamW.println(indent + "By: " + ref.authors() + " [[br]]");
        // Title
        refstreamW.println(indent + "'''" + ref.title() + "'''" + " [[br]]");
        // Booktitle
        refstreamW.println(indent + "In: " + ref.booktitle() + " [[br]]");
        // URL
        if(ref.url().length() > 0) {
          refstreamW.println(indent + "Online: [" + ref.url() + "][[br]]");
        }
      }
      refstreamW.println("");
      refstreamW.println("");
    }
  }

  private static List<Pair<Reference, List<Class<?>>>> sortedReferences() {
    List<Pair<Reference, List<Class<?>>>> refs = new ArrayList<Pair<Reference, List<Class<?>>>>();
    Map<Reference, List<Class<?>>> map = new HashMap<Reference, List<Class<?>>>();

    for(final Class<?> cls : InspectionUtil.findAllImplementations(Object.class, false)) {
      inspectClass(cls, refs, map);
    }
    return refs;
  }

  private static void inspectClass(final Class<?> cls, List<Pair<Reference, List<Class<?>>>> refs, Map<Reference, List<Class<?>>> map) {
    try {
      if(cls.isAnnotationPresent(Reference.class)) {
        Reference ref = cls.getAnnotation(Reference.class);
        List<Class<?>> list = map.get(ref);
        if(list == null) {
          list = new ArrayList<Class<?>>(5);
          map.put(ref, list);
          refs.add(new Pair<Reference, List<Class<?>>>(ref, list));
        }
        list.add(cls);
      }
      // Inner classes
      for(Class<?> c2 : cls.getDeclaredClasses()) {
        inspectClass(c2, refs, map);
      }
      for(Method m : cls.getDeclaredMethods()) {
        if(m.isAnnotationPresent(Reference.class)) {
          Reference ref = m.getAnnotation(Reference.class);
          List<Class<?>> list = map.get(ref);
          if(list == null) {
            list = new ArrayList<Class<?>>(5);
            map.put(ref, list);
            refs.add(new Pair<Reference, List<Class<?>>>(ref, list));
          }
          list.add(cls);
        }
      }
    }
    catch(NoClassDefFoundError e) {
      if(!cls.getCanonicalName().startsWith("experimentalcode.")) {
        logger.warning("Exception in finding references for class " + cls.getCanonicalName() + " - missing referenced class?");
      }
    }
    catch(Error e) {
      logger.warning("Exception in finding references for class " + cls.getCanonicalName() + ": " + e, e);
    }
  }

  private static String linkForClassName(String name) {
    String link = name.replace(".", "/") + ".html";
    return link;
  }

  /**
   * Fin all classes that have the reference annotation
   * 
   * @return All classes with the reference annotation.
   */
  public static ArrayList<Class<?>> findAllClassesWithReferences() {
    ArrayList<Class<?>> references = new ArrayList<Class<?>>();
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Object.class, false)) {
      if(cls.isAnnotationPresent(Reference.class)) {
        references.add(cls);
      }
      else {
        for(Method m : cls.getDeclaredMethods()) {
          if(m.isAnnotationPresent(Reference.class)) {
            references.add(cls);
          }
        }
      }
    }
    return references;
  }
}
