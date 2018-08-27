/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.application.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil;

/**
 * Build a reference documentation for all available parameters.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @apiviz.uses Reference
 */
public class DocumentReferences {
  private static final String DOIPREFIX = "https://doi.org/";

  private static final String DBLPPREFIX = "DBLP:";

  private static final String DBLPURL = "https://dblp.uni-trier.de/rec/bibtex/";

  private static final String CSSFILE = "stylesheet.css";

  private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(DocumentReferences.class);

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    if(args.length < 1 || args.length > 2) {
      LOG.warning("I need exactly one or two file names to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html") || (args.length > 1 && !args[1].endsWith(".md"))) {
      LOG.warning("File name doesn't end in expected extension!");
      System.exit(1);
    }

    List<Map.Entry<Reference, TreeSet<Object>>> refs = sortedReferences();
    File references = new File(args[0]);
    try (FileOutputStream reffo = new FileOutputStream(references); //
        OutputStream refstream = new BufferedOutputStream(reffo)) {
      Document refdoc = documentReferences(refs);
      HTMLUtil.writeXHTML(refdoc, refstream);
    }
    catch(IOException e) {
      LOG.exception("IO Exception writing HTML output.", e);
      System.exit(1);
    }
    if(args.length > 1) {
      File refwiki = new File(args[1]);
      try (FileOutputStream reffow = new FileOutputStream(refwiki); //
          MarkdownDocStream refstreamW = new MarkdownDocStream(reffow)) {
        documentReferencesMarkdown(refs, refstreamW);
      }
      catch(IOException e) {
        LOG.exception("IO Exception writing Wiki output.", e);
        System.exit(1);
      }
    }
  }

  private static Document documentReferences(List<Map.Entry<Reference, TreeSet<Object>>> refs) throws IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch(ParserConfigurationException e1) {
      throw new IOException(e1);
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
    head.appendChild(htmldoc.createComment(MODIFICATION_WARNING));
    body.appendChild(htmldoc.createComment(MODIFICATION_WARNING));
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
    for(Map.Entry<Reference, TreeSet<Object>> pair : refs) {
      // DT = definition term
      Element classdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        boolean first = true;
        for(Object o : pair.getValue()) {
          if(!first) {
            classdt.appendChild(htmldoc.createTextNode(", "));
          }
          if(o instanceof Class<?>) {
            Class<?> cls = (Class<?>) o;
            Element classan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
            classan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, cls.getName());
            classdt.appendChild(classan);

            // Link back to original class
            Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
            classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkFor(cls));
            classa.setTextContent(cls.getName());
            classdt.appendChild(classa);
          }
          else if(o instanceof Package) {
            Package pkg = (Package) o;
            Element classan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
            classan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, pkg.getName());
            classdt.appendChild(classan);

            // Link back to original class
            Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
            classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkFor(pkg));
            classa.setTextContent(pkg.getName());
            classdt.appendChild(classa);
          }

          first = false;
        }
      }
      maindl.appendChild(classdt);
      // DD = definition description
      Element classdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      maindl.appendChild(classdd);

      Reference ref = pair.getKey();
      // Prefix
      if(!ref.prefix().isEmpty()) {
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
      if(!ref.booktitle().isEmpty()) {
        Element booktitlediv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
        booktitlediv.setTextContent("In: " + ref.booktitle());
        if(ref.booktitle().startsWith("Online:")) {
          booktitlediv.setTextContent(ref.booktitle());
        }
        classdd.appendChild(booktitlediv);
      }
      // URL
      if(!ref.url().isEmpty()) {
        Element urldiv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
        Element urla = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        urla.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, ref.url());
        urla.setTextContent(ref.url());
        urldiv.appendChild(urla);
        classdd.appendChild(urldiv);
      }
      // Bibkey
      if(!ref.bibkey().isEmpty()) {
        if(ref.bibkey().startsWith(DBLPPREFIX)) {
          Element urldiv = htmldoc.createElement(HTMLUtil.HTML_DIV_TAG);
          Element urla = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          urla.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, DBLPURL + ref.bibkey());
          urla.setTextContent(ref.url());
          urldiv.appendChild(urla);
          classdd.appendChild(urldiv);
        }
        else {
          classdd.appendChild((Element) htmldoc.createComment(ref.bibkey()));
        }
      }
    }
    return htmldoc;
  }

  private static void documentReferencesMarkdown(List<Map.Entry<Reference, TreeSet<Object>>> refs, MarkdownDocStream refstreamW) {
    for(Map.Entry<Reference, TreeSet<Object>> pair : refs) {
      // JavaDoc links for relevant classes and packages.
      boolean first = true;
      for(Object o : pair.getValue()) {
        if(!first) {
          refstreamW.append(',').lf();
        }
        if(o instanceof Class<?>) {
          Class<?> cls = (Class<?>) o;
          refstreamW.append('[').append(cls.getName()).append("](./releases/current/doc/") //
              .append(linkFor(cls)).append(')');
        }
        else if(o instanceof Package) {
          Package pkg = (Package) o;
          refstreamW.append('[').append(pkg.getName()).append("](./releases/current/doc/") //
              .append(linkFor(pkg)).append(')');
        }
        first = false;
      }
      refstreamW.lf();

      Reference ref = pair.getKey();
      // Prefix
      if(!ref.prefix().isEmpty()) {
        refstreamW.escaped(ref.prefix()).lf();
      }
      // Authors
      refstreamW //
          // authors
          .append(ref.authors()).lf() //
          // Title
          .append("**").escaped(ref.title()).append("**").lf();
      // Booktitle
      if(!ref.booktitle().isEmpty() && !ref.booktitle().equals(ref.url()) && !ref.booktitle().equals("Online")) {
        refstreamW.append(ref.booktitle().startsWith("Online:") ? "" : "In: ").escaped(ref.booktitle()).lf();
      }
      // URL
      if(!ref.url().isEmpty()) {
        if(ref.url().startsWith(DOIPREFIX)) {
          refstreamW.append("[DOI:").append(ref.url(), DOIPREFIX.length(), ref.url().length())//
              .append("](").append(ref.url()).append(')').lf();
        }
        else {
          refstreamW.append("Online: <").append(ref.url()).append('>').lf();
        }
      }
      // Bibkey, if we can link to DBLP:
      if(!ref.bibkey().isEmpty()) {
        if(ref.bibkey().startsWith(DBLPPREFIX)) {
          refstreamW.append("[DBLP:").append(ref.bibkey(), DBLPPREFIX.length(), ref.bibkey().length())//
              .append("](").append(DBLPURL)//
              .append(ref.bibkey(), DBLPPREFIX.length(), ref.bibkey().length()).append(')').lf();
        }
        else {
          refstreamW.append("<!-- ").append(ref.bibkey()).append(" -->").lf();
        }
      }
      refstreamW.par();
    }
  }

  private static List<Map.Entry<Reference, TreeSet<Object>>> sortedReferences() {
    Map<Reference, TreeSet<Object>> map = new HashMap<>();

    HashSet<Package> packages = new HashSet<>();
    for(Class<?> cls : ELKIServiceRegistry.findAllImplementations(Object.class, true, false)) {
      inspectClass(cls, map);
      if(packages.add(cls.getPackage())) {
        inspectPackage(cls.getPackage(), map);
      }
    }
    // Sort references by first class.
    List<Map.Entry<Reference, TreeSet<Object>>> refs = new ArrayList<>(map.entrySet());
    Collections.sort(refs, new Comparator<Map.Entry<Reference, TreeSet<Object>>>() {
      @Override
      public int compare(Map.Entry<Reference, TreeSet<Object>> p1, Map.Entry<Reference, TreeSet<Object>> p2) {
        final Object o1 = p1.getValue().first(), o2 = p2.getValue().first();
        int c = COMPARATOR.compare(o1, o2);
        if(c == 0) {
          Reference r1 = p1.getKey(), r2 = p2.getKey();
          c = compareNull(r1.title(), r2.title());
          c = (c != 0) ? c : compareNull(r1.authors(), r2.authors());
          c = (c != 0) ? c : compareNull(r1.booktitle(), r2.booktitle());
        }
        return c;
      }

      /**
       * Null-tolerant String comparison.
       * 
       * @param s1 First string
       * @param s2 Second string
       * @return Order
       */
      private int compareNull(String s1, String s2) {
        return (s1 == s2) ? 0 //
            : (s1 == null) ? -1 //
                : (s2 == null) ? +1 //
                    : s1.compareTo(s2);
      }
    });
    return refs;
  }

  /**
   * Comparator for sorting the list of classes for each reference.
   */
  private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2) {
      String n1 = (o1 instanceof Class) ? ((Class<?>) o1).getName() : ((Package) o1).getName();
      String n2 = (o2 instanceof Class) ? ((Class<?>) o2).getName() : ((Package) o2).getName();
      return n1.compareTo(n2);
    }
  };

  private static void inspectClass(final Class<?> cls, Map<Reference, TreeSet<Object>> map) {
    if(cls.getSimpleName().equals("package-info")) {
      return;
    }
    try {
      if(cls.isAnnotationPresent(Reference.class)) {
        addReference(cls, cls.getAnnotationsByType(Reference.class), map);
      }
      // Inner classes
      for(Class<?> c2 : cls.getDeclaredClasses()) {
        inspectClass(c2, map);
      }
      for(Method m : cls.getDeclaredMethods()) {
        if(m.isAnnotationPresent(Reference.class)) {
          addReference(cls, m.getAnnotationsByType(Reference.class), map);
        }
      }
      for(Field f : cls.getDeclaredFields()) {
        if(f.isAnnotationPresent(Reference.class)) {
          addReference(cls, f.getAnnotationsByType(Reference.class), map);
        }
      }
    }
    catch(Error e) {
      LOG.warning("Exception in finding references for class " + cls.getCanonicalName() + ": " + e, e);
    }
  }

  private static void addReference(Object cls, Reference[] r, Map<Reference, TreeSet<Object>> map) {
    for(Reference ref : r) {
      TreeSet<Object> list = map.get(ref);
      if(list == null) {
        map.put(ref, list = new TreeSet<>(COMPARATOR));
      }
      list.add(cls);
    }
  }

  private static void inspectPackage(Package p, Map<Reference, TreeSet<Object>> map) {
    if(p.isAnnotationPresent(Reference.class)) {
      addReference(p, p.getAnnotationsByType(Reference.class), map);
    }
  }

  private static String linkFor(Class<?> cls) {
    return cls.getName().replace('.', '/') + ".html";
  }

  private static String linkFor(Package name) {
    return name.getName().replace('.', '/') + "/package-summary.html";
  }

  /**
   * Find all classes that have the reference annotation
   *
   * @return All classes with the reference annotation.
   */
  public static ArrayList<Class<?>> findAllClassesWithReferences() {
    ArrayList<Class<?>> references = new ArrayList<>();
    for(final Class<?> cls : ELKIServiceRegistry.findAllImplementations(Object.class, true, false)) {
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
