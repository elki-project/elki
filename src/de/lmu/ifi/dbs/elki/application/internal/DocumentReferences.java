package de.lmu.ifi.dbs.elki.application.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.DocumentationUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil;

/**
 * Build a reference documentation for all available parameters.
 * 
 * @author Erich Schubert
 */
public class DocumentReferences {
  private static final String CSSFILE = "stylesheet.css";

  private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

  /**
   * @param args
   */
  public static void main(String[] args) {
    if(args.length != 1) {
      LoggingUtil.warning("I need exactly one file name to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html")) {
      LoggingUtil.warning("File name doesn't end with .html!");
      System.exit(1);
    }
    File references = new File(args[0]);

    {
      FileOutputStream reffo;
      try {
        reffo = new FileOutputStream(references);
      }
      catch(FileNotFoundException e) {
        LoggingUtil.exception("Can't create output stream!", e);
        throw new RuntimeException(e);
      }
      OutputStream refstream = new BufferedOutputStream(reffo);
      Document refdoc = documentParameters();
      try {
        HTMLUtil.writeXHTML(refdoc, refstream);
        refstream.flush();
        refstream.close();
        reffo.close();
      }
      catch(IOException e) {
        LoggingUtil.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }
  }

  private static Document documentParameters() {
    ArrayList<Class<?>> classes = findAllClassesWithReferences();
    Collections.sort(classes, new InspectionUtil.ClassSorter());

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
    for(Class<?> cls : classes) {
      // DT = definition term
      Element classdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        Element classan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        classan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, cls.getName());
        classdt.appendChild(classan);
      }
      // Link back to original class
      {
        Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(cls.getName()));
        classa.setTextContent(cls.getName());
        classdt.appendChild(classa);
      }
      maindl.appendChild(classdt);
      // DD = definition description
      Element classdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      maindl.appendChild(classdd);

      {
        Reference ref = DocumentationUtil.getReference(cls);
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
        booktitlediv.setTextContent("In: "+ref.booktitle());
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
    }
    return references;
  }
}
