package de.lmu.ifi.dbs.elki.application.internal;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceScanner;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.UnParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil;

/**
 * Class to generate HTML parameter descriptions for all classes that have ELKI
 * {@link Parameter}s. Used in documentation generation only.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @apiviz.uses Parameter
 */
public class DocumentParameters {
  private static final Logging LOG = Logging.getLogger(DocumentParameters.class);

  private static final String HEADER_PARAMETER_FOR = "Parameter for: ";

  private static final String HEADER_DEFAULT_VALUE = "Default: ";

  private static final String NO_DEFAULT_VALUE = "No default value.";

  private static final String HEADER_CLASS_RESTRICTION = "Class Restriction: ";

  private static final String HEADER_CLASS_RESTRICTION_IMPLEMENTING = "implements ";

  private static final String HEADER_CLASS_RESTRICTION_EXTENDING = "extends ";

  private static final String NO_CLASS_RESTRICTION = "No class restriction.";

  private static final String CSSFILE = "stylesheet.css";

  private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

  private static final String HEADER_KNOWN_IMPLEMENTATIONS = "Known implementations: ";

  /**
   * Enable the full wiki output. Currently not sensible, as there is a size
   * restriction on wiki pages, so we would need to split this somehow!
   */
  private static final boolean FULL_WIKI_OUTPUT = false;

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    LoggingConfiguration.setVerbose(Level.VERBOSE);
    if(args.length != 2 && args.length != 4) {
      LOG.warning("I need exactly two or four file names to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html")) {
      LOG.warning("First file name doesn't end with .html!");
      System.exit(1);
    }
    if(!args[1].endsWith(".html")) {
      LOG.warning("Second file name doesn't end with .html!");
      System.exit(1);
    }
    if(args.length > 2 && !args[2].endsWith(".trac")) {
      LOG.warning("Third file name doesn't end with .trac!");
      System.exit(1);
    }
    if(args.length > 3 && !args[3].endsWith(".trac")) {
      LOG.warning("Fourth file name doesn't end with .trac!");
      System.exit(1);
    }
    File byclsname = new File(args[0]);
    File byoptname = new File(args[1]);
    File byclsnamew = args.length >= 3 ? new File(args[2]) : null;
    File byoptnamew = args.length >= 4 ? new File(args[3]) : null;

    Map<Class<?>, List<Parameter<?>>> byclass = new HashMap<>();
    Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt = new HashMap<>();
    try {
      buildParameterIndex(byclass, byopt);
    }
    catch(Exception e) {
      LOG.exception(e);
      System.exit(1);
    }

    try (FileOutputStream byclassfo = new FileOutputStream(byclsname); //
        OutputStream byclassstream = new BufferedOutputStream(byclassfo)) {
      Document byclassdoc = makeByClassOverviewHTML(byclass);
      HTMLUtil.writeXHTML(byclassdoc, byclassstream);
    }
    catch(IOException e) {
      LOG.exception("IO Exception writing output.", e);
      System.exit(1);
    }
    if(byclsnamew != null) {
      try (FileOutputStream byclassfo = new FileOutputStream(byclsnamew); //
          PrintStream byclassstream = new PrintStream(new BufferedOutputStream(byclassfo), false, "UTF-8")) {
        makeByClassOverviewWiki(byclass, new WikiStream(byclassstream));
      }
      catch(IOException e) {
        LOG.exception("IO Exception writing output.", e);
        System.exit(1);
      }
    }

    try (FileOutputStream byoptfo = new FileOutputStream(byoptname); //
        OutputStream byoptstream = new BufferedOutputStream(byoptfo)) {
      Document byoptdoc = makeByOptOverviewHTML(byopt);
      HTMLUtil.writeXHTML(byoptdoc, byoptfo);
    }
    catch(IOException e) {
      LOG.exception("IO Exception writing output.", e);
      System.exit(1);
    }

    if(byoptnamew != null) {
      try (FileOutputStream byoptfo = new FileOutputStream(byoptnamew); //
          PrintStream byoptstream = new PrintStream(new BufferedOutputStream(byoptfo))) {
        makeByOptOverviewWiki(byopt, new WikiStream(byoptstream));
      }
      catch(IOException e) {
        LOG.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }
    // Forcibly terminate, as some class may have spawned a thread.
    System.exit(0);
  }

  private static void buildParameterIndex(Map<Class<?>, List<Parameter<?>>> byclass, Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt) {
    final ArrayList<TrackedParameter> options = new ArrayList<>();
    ExecutorService es = Executors.newSingleThreadExecutor();
    List<Class<?>> objs = ELKIServiceRegistry.findAllImplementations(Object.class, false, true);
    Collections.sort(objs, new ELKIServiceScanner.ClassSorter());
    Class<?> appc = appBaseClass();
    for(final Class<?> cls : objs) {
      // Doesn't have a proper name?
      if(cls.getCanonicalName() == null) {
        continue;
      }
      // Some of the "applications" do currently not have appropriate
      // constructors / parameterizers and may start AWT threads - skip them.
      if(appc != null && appc.isAssignableFrom(cls)) {
        continue;
      }

      UnParameterization config = new UnParameterization();
      TrackParameters track = new TrackParameters(config, cls);
      try {
        // Wait up to one second.
        es.submit(new FutureTask<Object>(//
            new Instancer(cls, track, options), null))//
            .get(1L, TimeUnit.SECONDS);
      }
      catch(TimeoutException e) {
        LOG.warning("Timeout on instantiating " + cls.getName());
        es.shutdownNow();
        throw new RuntimeException(e);
      }
      catch(Exception e) {
        LOG.warning("Error instantiating " + cls.getName(), e.getCause());
        continue;
      }
    }
    LOG.debug("Documenting " + options.size() + " parameter instances.");
    for(TrackedParameter pp : options) {
      if(pp.getOwner() == null || pp.getParameter() == null) {
        LOG.debugFiner("Null: " + pp.getOwner() + " " + pp.getParameter());
        continue;
      }
      Class<?> c = (Class<?>) ((pp.getOwner() instanceof Class) ? pp.getOwner() : pp.getOwner().getClass());
      Parameter<?> o = pp.getParameter();

      // just collect unique occurrences
      {
        List<Parameter<?>> byc = byclass.get(c);
        boolean inlist = false;
        if(byc != null) {
          for(Parameter<?> par : byc) {
            if(par.getOptionID() == o.getOptionID()) {
              inlist = true;
              break;
            }
          }
        }
        if(!inlist) {
          List<Parameter<?>> ex = byclass.get(c);
          if(ex == null) {
            byclass.put(c, ex = new ArrayList<>());
          }
          ex.add(o);
        }
      }
      {
        List<Pair<Parameter<?>, Class<?>>> byo = byopt.get(o.getOptionID());
        boolean inlist = false;
        if(byo != null) {
          for(Pair<Parameter<?>, Class<?>> pair : byo) {
            if(pair.second.equals(c)) {
              inlist = true;
              break;
            }
          }
        }
        if(!inlist) {
          List<Pair<Parameter<?>, Class<?>>> ex = byopt.get(o.getOptionID());
          if(ex == null) {
            byopt.put(o.getOptionID(), ex = new ArrayList<>());
          }
          ex.add(new Pair<Parameter<?>, Class<?>>(o, c));
        }
      }
    }
  }

  /**
   * Get the base application class (to be ignored).
   *
   * @return Application class.
   */
  private static Class<?> appBaseClass() {
    try {
      return Class.forName("de.lmu.ifi.dbs.elki.application.AbstractApplication");
    }
    catch(ClassNotFoundException e) {
      return null;
    }
  }

  protected static Constructor<?> getConstructor(final Class<?> cls) {
    try {
      return cls.getConstructor(Parameterization.class);
    }
    catch(java.lang.NoClassDefFoundError e) {
      return null;
    }
    catch(RuntimeException e) {
      // Not parameterizable, usually not even found ...
      LOG.warning("RuntimeException: ", e);
      return null;
    }
    catch(Exception e) {
      // Not parameterizable.
      return null;
    }
    catch(java.lang.Error e) {
      // Not parameterizable.
      LOG.warning("Error: ", e);
      return null;
    }
  }

  private static class Instancer implements Runnable {
    /**
     * Class to instantiate.
     */
    private Class<?> cls;

    /**
     * Parameter tracking helper.
     */
    private TrackParameters track;

    /**
     * Options list.
     */
    private ArrayList<TrackedParameter> options;

    /**
     * Constructor.
     *
     * @param cls Class to instantiate
     * @param track Parameter tracking helper
     * @param options Options list.
     */
    public Instancer(Class<?> cls, TrackParameters track, ArrayList<TrackedParameter> options) {
      this.cls = cls;
      this.track = track;
      this.options = options;
    }

    @Override
    public void run() {
      // Only support V3 style parameterizers now:
      Parameterizer par = ClassGenericsUtil.getParameterizer(cls);
      if(par != null) {
        par.configure(track);
        for(TrackedParameter pair : track.getAllParameters()) {
          if(pair.getOwner() == null) {
            LOG.warning("No owner for parameter " + pair.getParameter() + " expected a " + cls.getName());
            continue;
          }
          options.add(pair);
        }
      }
      // Not parameterizable.
    }
  }

  private static Document makeByClassOverviewHTML(Map<Class<?>, List<Parameter<?>>> byclass) {
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
      title.setTextContent("Command line parameter overview.");
      head.appendChild(title);
    }
    // Heading
    {
      Element h1 = htmldoc.createElement(HTMLUtil.HTML_H1_TAG);
      h1.setTextContent("ELKI command line parameter overview:");
      body.appendChild(h1);
    }

    // Main definition list
    Element maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
    body.appendChild(maindl);

    List<Class<?>> classes = new ArrayList<>(byclass.keySet());
    Collections.sort(classes, new ELKIServiceScanner.ClassSorter());

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
      // nested definition list for options
      Element classdl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
      classdd.appendChild(classdl);
      for(Parameter<?> opt : byclass.get(cls)) {
        // DT definition term: option name, in TT for typewriter optics
        Element elemdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
        {
          Element elemtt = htmldoc.createElement(HTMLUtil.HTML_TT_TAG);
          elemtt.setTextContent(SerializedParameterization.OPTION_PREFIX + opt.getName() + " " + opt.getSyntax());
          elemdt.appendChild(elemtt);
        }
        classdl.appendChild(elemdt);
        // DD definition description - put the option description here.
        Element elemdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
        Element elemp = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        if(opt.getShortDescription() != null) {
          HTMLUtil.appendMultilineText(htmldoc, elemp, opt.getShortDescription());
        }
        elemdd.appendChild(elemp);
        // class restriction?
        if(opt instanceof ClassParameter<?>) {
          appendClassRestriction(htmldoc, ((ClassParameter<?>) opt).getRestrictionClass(), elemdd);
        }
        // default value? completions?
        appendDefaultValueIfSet(htmldoc, opt, elemdd);
        // known values?
        if(opt instanceof ClassParameter<?>) {
          Class<?> restriction = ((ClassParameter<?>) opt).getRestrictionClass();
          appendKnownImplementationsIfNonempty(htmldoc, restriction, elemdd);
        }
        else if(opt instanceof ClassListParameter<?>) {
          Class<?> restriction = ((ClassListParameter<?>) opt).getRestrictionClass();
          appendKnownImplementationsIfNonempty(htmldoc, restriction, elemdd);
        }
        classdl.appendChild(elemdd);
      }
    }
    return htmldoc;
  }

  /**
   * Write to a Wiki format.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private static class WikiStream {
    PrintStream out;

    int indent = 0;

    // Newline mode.
    int newline = 0;

    WikiStream(PrintStream out) {
      this.out = out;
      this.newline = -1;
    }

    WikiStream print(String p) {
      insertNewline();
      out.print(p);
      return this;
    }

    WikiStream print(char c) {
      insertNewline();
      out.print(c);
      return this;
    }

    WikiStream insertNewline() {
      if(newline == 2) {
        out.print("[[br]]");
      }
      if(newline != 0) {
        printIndent();
        newline = 0;
      }
      return this;
    }

    WikiStream printIndent() {
      if(newline > 0) {
        out.println();
      }
      for(int i = indent; i > 0; i--) {
        out.print(' ');
      }
      return this;
    }

    WikiStream println(String p) {
      insertNewline();
      out.print(p);
      newline = 2;
      return this;
    }

    WikiStream println() {
      newline = 2;
      return this;
    }

    WikiStream printitem(String item) {
      printIndent();
      newline = 0;
      print(item);
      return this;
    }

    WikiStream javadocLink(Class<?> cls, Class<?> base) {
      insertNewline();
      out.print("[[javadoc(");
      out.print(cls.getCanonicalName());
      if(base != null) {
        out.print(',');
        out.print(ClassParameter.canonicalClassName(cls, base));
      }
      out.print(")]]");
      return this;
    }
  }

  private static void makeByClassOverviewWiki(Map<Class<?>, List<Parameter<?>>> byclass, WikiStream out) {
    List<Class<?>> classes = new ArrayList<>(byclass.keySet());
    Collections.sort(classes, new ELKIServiceScanner.ClassSorter());
    Class<?> base = getBaseClass();

    for(Class<?> cls : classes) {
      out.indent = 0;
      out.printitem("'''").javadocLink(cls, base).println("''':");
      out.indent = 1;
      out.newline = 1; // No BR needed, we increase the indent.
      for(Parameter<?> opt : byclass.get(cls)) {
        out.printitem("* ");
        out.print("{{{").print(SerializedParameterization.OPTION_PREFIX) //
            .print(opt.getName()).print(' ').print(opt.getSyntax()) //
            .println("}}}");
        if(opt.getShortDescription() != null) {
          appendMultilineTextWiki(out, opt.getShortDescription());
        }
        // class restriction?
        if(opt instanceof ClassParameter<?>) {
          appendClassRestrictionWiki(out, ((ClassParameter<?>) opt).getRestrictionClass());
        }
        // default value?
        if(opt.hasDefaultValue()) {
          appendDefaultValueWiki(out, opt);
        }
        // known values?
        if(FULL_WIKI_OUTPUT) {
          if(opt instanceof ClassParameter<?>) {
            Class<?> restriction = ((ClassParameter<?>) opt).getRestrictionClass();
            appendKnownImplementationsWiki(out, restriction);
          }
          else if(opt instanceof ClassListParameter<?>) {
            Class<?> restriction = ((ClassListParameter<?>) opt).getRestrictionClass();
            appendKnownImplementationsWiki(out, restriction);
          }
        }
      }
    }
  }

  /**
   * Get the base class, for naming.
   *
   * @return Base class.
   */
  private static Class<?> getBaseClass() {
    try {
      return Class.forName("de.lmu.ifi.dbs.elki.KDDTask");
    }
    catch(ClassNotFoundException e) {
      return null; // Just worse links, not serious.
    }
  }

  private static void appendMultilineTextWiki(WikiStream out, String text) {
    for(String line : text.split("\n")) {
      out.println(line);
    }
  }

  private static Document makeByOptOverviewHTML(Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt) throws IOException {
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
      title.setTextContent("Command line parameter overview - by option");
      head.appendChild(title);
    }
    // Heading
    {
      Element h1 = htmldoc.createElement(HTMLUtil.HTML_H1_TAG);
      h1.setTextContent("ELKI command line parameter overview:");
      body.appendChild(h1);
    }

    // Main definition list
    Element maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
    body.appendChild(maindl);

    final Comparator<OptionID> osort = new SortByOption();
    final Comparator<Class<?>> csort = new ELKIServiceScanner.ClassSorter();
    Comparator<Pair<Parameter<?>, Class<?>>> psort = new Comparator<Pair<Parameter<?>, Class<?>>>() {
      @Override
      public int compare(Pair<Parameter<?>, Class<?>> o1, Pair<Parameter<?>, Class<?>> o2) {
        int c = osort.compare(o1.first.getOptionID(), o2.first.getOptionID());
        return (c != 0) ? c : csort.compare(o1.second, o2.second);
      }
    };

    List<OptionID> opts = new ArrayList<>(byopt.keySet());
    Collections.sort(opts, osort);
    for(OptionID oid : opts) {
      final Parameter<?> firstopt = byopt.get(oid).get(0).getFirst();
      // DT = definition term
      Element optdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        Element optan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        optan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, firstopt.getName());
        optdt.appendChild(optan);
      }
      // option name
      {
        Element elemtt = htmldoc.createElement(HTMLUtil.HTML_TT_TAG);
        elemtt.setTextContent(SerializedParameterization.OPTION_PREFIX + firstopt.getName() + " " + firstopt.getSyntax());
        optdt.appendChild(elemtt);
      }
      maindl.appendChild(optdt);
      // DD = definition description
      Element optdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      {
        Element elemp = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        HTMLUtil.appendMultilineText(htmldoc, elemp, firstopt.getShortDescription());
        optdd.appendChild(elemp);
      }
      // class restriction?
      Class<?> superclass = getRestrictionClass(oid, firstopt, byopt);
      if(superclass != null) {
        appendClassRestriction(htmldoc, superclass, optdd);
      }
      // default value?
      appendDefaultValueIfSet(htmldoc, firstopt, optdd);
      // known values?
      if(superclass != null) {
        appendKnownImplementationsIfNonempty(htmldoc, superclass, optdd);
      }
      maindl.appendChild(optdd);
      // nested definition list for options
      Element classesul = htmldoc.createElement(HTMLUtil.HTML_UL_TAG);
      {
        Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        p.appendChild(htmldoc.createTextNode(HEADER_PARAMETER_FOR));
        optdd.appendChild(p);
      }
      optdd.appendChild(classesul);
      List<Pair<Parameter<?>, Class<?>>> plist = byopt.get(oid);
      Collections.sort(plist, psort);
      for(Pair<Parameter<?>, Class<?>> clinst : plist) {
        // DT definition term: option name, in TT for typewriter optics
        Element classli = htmldoc.createElement(HTMLUtil.HTML_LI_TAG);

        // Link back to original class
        {
          Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(clinst.getSecond().getName()));
          classa.setTextContent(clinst.getSecond().getName());
          classli.appendChild(classa);
        }
        if(clinst.getFirst() instanceof ClassParameter<?> && firstopt instanceof ClassParameter<?>) {
          ClassParameter<?> cls = (ClassParameter<?>) clinst.getFirst();
          if(cls.getRestrictionClass() != null) {
            // TODO: if it is null, it could still be different!
            if(!cls.getRestrictionClass().equals(superclass)) {
              appendClassRestriction(htmldoc, cls.getRestrictionClass(), classli);
            }
          }
          else {
            appendNoClassRestriction(htmldoc, classli);
          }
        }
        Parameter<?> param = clinst.getFirst();
        if(param.getDefaultValue() != null) {
          if(!param.getDefaultValue().equals(firstopt.getDefaultValue())) {
            appendDefaultValueIfSet(htmldoc, param, classli);
          }
        }
        else if(firstopt.getDefaultValue() != null) {
          appendNoDefaultValue(htmldoc, classli);
        }
        classesul.appendChild(classli);
      }
    }
    return htmldoc;
  }

  /**
   * Get the restriction class of an option.
   *
   * @param oid
   * @param firstopt
   * @param byopt
   * @return
   */
  private static Class<?> getRestrictionClass(OptionID oid, final Parameter<?> firstopt, Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt) {
    Class<?> superclass;
    if(firstopt instanceof ClassParameter<?>) {
      superclass = ((ClassParameter<?>) firstopt).getRestrictionClass();
    }
    else if(firstopt instanceof ClassListParameter<?>) {
      superclass = ((ClassListParameter<?>) firstopt).getRestrictionClass();
    }
    else {
      return null;
    }
    // Also look for more general restrictions:
    for(Pair<Parameter<?>, Class<?>> clinst : byopt.get(oid)) {
      if(clinst.getFirst() instanceof ClassParameter) {
        ClassParameter<?> cls = (ClassParameter<?>) clinst.getFirst();
        if(!cls.getRestrictionClass().equals(superclass) && cls.getRestrictionClass().isAssignableFrom(superclass)) {
          superclass = cls.getRestrictionClass();
        }
      }
      if(clinst.getFirst() instanceof ClassListParameter) {
        ClassListParameter<?> cls = (ClassListParameter<?>) clinst.getFirst();
        if(!cls.getRestrictionClass().equals(superclass) && cls.getRestrictionClass().isAssignableFrom(superclass)) {
          superclass = cls.getRestrictionClass();
        }
      }
    }
    return superclass;
  }

  private static void makeByOptOverviewWiki(Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt, WikiStream out) {
    List<OptionID> opts = new ArrayList<>(byopt.keySet());
    Collections.sort(opts, new SortByOption());

    for(OptionID oid : opts) {
      final Parameter<?> firstopt = byopt.get(oid).get(0).getFirst();
      out.indent = 1;
      out.printitem("").print("{{{").print(SerializedParameterization.OPTION_PREFIX) //
          .print(firstopt.getName()).print(' ').print(firstopt.getSyntax()).println("}}}:: ");
      out.newline = 1; // No BR needed, we increase the indent.
      out.indent = 2;

      appendMultilineTextWiki(out, firstopt.getShortDescription());
      // class restriction?
      Class<?> superclass = getRestrictionClass(oid, firstopt, byopt);
      if(superclass != null) {
        appendClassRestrictionWiki(out, superclass);
      }
      // default value?
      if(firstopt.hasDefaultValue()) {
        appendDefaultValueWiki(out, firstopt);
      }
      if(FULL_WIKI_OUTPUT) {
        // known values?
        if(superclass != null) {
          appendKnownImplementationsWiki(out, superclass);
        }
        // List of classes that use this parameter
        out.println("Used by:");
        for(Pair<Parameter<?>, Class<?>> clinst : byopt.get(oid)) {
          out.indent = 3;
          out.printitem("* ").javadocLink(clinst.getSecond(), null).println();
          if(clinst.getFirst() instanceof ClassParameter<?> && firstopt instanceof ClassParameter<?>) {
            ClassParameter<?> cls = (ClassParameter<?>) clinst.getFirst();
            if(cls.getRestrictionClass() != null) {
              // TODO: if it is null, it could still be different!
              if(!cls.getRestrictionClass().equals(superclass)) {
                appendClassRestrictionWiki(out, cls.getRestrictionClass());
              }
            }
            else {
              appendNoClassRestrictionWiki(out);
            }
          }
          Parameter<?> param = clinst.getFirst();
          if(param.getDefaultValue() != null) {
            if(!param.getDefaultValue().equals(firstopt.getDefaultValue())) {
              appendDefaultValueWiki(out, param);
            }
          }
          else if(firstopt.getDefaultValue() != null) {
            appendNoDefaultValueWiki(out);
          }
          out.println();
        }
      }
    }
  }

  private static void appendDefaultClassLink(Document htmldoc, Parameter<?> opt, Element p) {
    Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
    defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(((ClassParameter<?>) opt).getDefaultValue().getCanonicalName()));
    defa.setTextContent(((ClassParameter<?>) opt).getDefaultValue().getCanonicalName());
    p.appendChild(defa);
  }

  private static void appendClassRestriction(Document htmldoc, Class<?> restriction, Element elemdd) {
    assert (restriction != null);
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
    if(restriction.isInterface()) {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_IMPLEMENTING));
    }
    else {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_EXTENDING));
    }
    Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
    defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(restriction.getName()));
    defa.setTextContent(restriction.getName());
    p.appendChild(defa);
    elemdd.appendChild(p);
  }

  private static void appendNoClassRestriction(Document htmldoc, Element elemdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
    p.appendChild(htmldoc.createTextNode(NO_CLASS_RESTRICTION));
    elemdd.appendChild(p);
  }

  private static void appendClassRestrictionWiki(WikiStream out, Class<?> restriction) {
    assert (restriction != null);
    out.print(HEADER_CLASS_RESTRICTION) //
        .print(restriction.isInterface() ? HEADER_CLASS_RESTRICTION_IMPLEMENTING : HEADER_CLASS_RESTRICTION_EXTENDING) //
        .javadocLink(restriction, null).println();
  }

  private static void appendNoClassRestrictionWiki(WikiStream out) {
    out.print(HEADER_CLASS_RESTRICTION).print(NO_CLASS_RESTRICTION).println();
  }

  private static void appendKnownImplementationsIfNonempty(Document htmldoc, Class<?> restriction, Element elemdd) {
    if(restriction != Object.class) {
      List<Class<?>> iter = ELKIServiceRegistry.findAllImplementations(restriction);
      if(!iter.isEmpty()) {
        Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        p.appendChild(htmldoc.createTextNode(HEADER_KNOWN_IMPLEMENTATIONS));
        elemdd.appendChild(p);
        Element ul = htmldoc.createElement(HTMLUtil.HTML_UL_TAG);
        for(Class<?> c : iter) {
          Element li = htmldoc.createElement(HTMLUtil.HTML_LI_TAG);
          Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(c.getName()));
          defa.setTextContent(ClassParameter.canonicalClassName(c, restriction));
          li.appendChild(defa);
          ul.appendChild(li);
        }
        elemdd.appendChild(ul);
      }
      // FIXME: The following currently cannot be used:
      // Report when not in properties file.
      // Iterator<Class<?>> clss = new
      // ELKIServiceLoader(opt.getRestrictionClass()).load();
      // if(!clss.hasNext() &&
      // !opt.getRestrictionClass().getName().startsWith("experimentalcode.")) {
      // LOG.warning(opt.getRestrictionClass().getName() + " not in properties.
      // No autocompletion available in release GUI.");
      // }
    }
  }

  private static void appendKnownImplementationsWiki(WikiStream out, Class<?> restriction) {
    List<Class<?>> implementations = ELKIServiceRegistry.findAllImplementations(restriction);
    if(implementations.isEmpty()) {
      return;
    }
    out.println(HEADER_KNOWN_IMPLEMENTATIONS);
    out.indent++;
    for(Class<?> c : implementations) {
      out.printitem("* ").javadocLink(c, restriction).println();
    }
    out.indent--;
  }

  /**
   * Append string containing the default value.
   *
   * @param htmldoc Document
   * @param par Parameter
   * @param optdd HTML Element
   */
  private static void appendDefaultValueIfSet(Document htmldoc, Parameter<?> par, Element optdd) {
    if(par.hasDefaultValue()) {
      Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
      p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
      if(par instanceof ClassParameter<?>) {
        appendDefaultClassLink(htmldoc, par, p);
      }
      else if(par instanceof RandomParameter && par.getDefaultValue() == RandomFactory.DEFAULT) {
        p.appendChild(htmldoc.createTextNode("use global random seed"));
      }
      else {
        p.appendChild(htmldoc.createTextNode(par.getDefaultValueAsString()));
      }
      optdd.appendChild(p);
    }
  }

  /**
   * Append string that there is not default value.
   *
   * @param htmldoc Document
   * @param optdd HTML Element
   */
  private static void appendNoDefaultValue(Document htmldoc, Element optdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
    p.appendChild(htmldoc.createTextNode(NO_DEFAULT_VALUE));
    optdd.appendChild(p);
  }

  private static void appendDefaultValueWiki(WikiStream out, Parameter<?> par) {
    out.print(HEADER_DEFAULT_VALUE);
    if(par instanceof ClassParameter<?>) {
      final Class<?> name = ((ClassParameter<?>) par).getDefaultValue();
      out.javadocLink(name, null).println();
    }
    else if(par instanceof RandomParameter && par.getDefaultValue() == RandomFactory.DEFAULT) {
      out.println("use global random seed");
    }
    else {
      out.println(par.getDefaultValueAsString());
    }
  }

  private static void appendNoDefaultValueWiki(WikiStream out) {
    out.print(HEADER_DEFAULT_VALUE).println(NO_DEFAULT_VALUE);
  }

  /**
   * Return a link for the class name
   *
   * @param name Class name
   * @return (relative) link destination
   */
  private static String linkForClassName(String name) {
    return name.replace('.', '/') + ".html";
  }

  /**
   * Sort parameters by their option
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  protected static class SortByOption implements Comparator<OptionID> {
    @Override
    public int compare(OptionID o1, OptionID o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
