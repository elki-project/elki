/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.application.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import elki.logging.Logging;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.ELKIServiceRegistry;
import elki.utilities.ELKIServiceScanner;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.*;
import elki.utilities.optionhandling.parameters.ClassListParameter;
import elki.utilities.optionhandling.parameters.ClassParameter;
import elki.utilities.optionhandling.parameters.Parameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.pairs.Pair;
import elki.utilities.random.RandomFactory;
import elki.utilities.xml.HTMLUtil;

/**
 * Class to generate HTML parameter descriptions for all classes that have ELKI
 * {@link Parameter}s. Used in documentation generation only.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @assoc - - - Parameter
 */
public class DocumentParameters {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(DocumentParameters.class);

  /** Header string */
  private static final String HEADER_PARAMETER_FOR = "Parameter for: ";

  /** Header string */
  private static final String HEADER_DEFAULT_VALUE = "Default: ";

  /** Header string */
  private static final String HEADER_CLASS_RESTRICTION = "Class Restriction: ";

  /** Header string */
  private static final String HEADER_CLASS_RESTRICTION_IMPLEMENTING = "implements ";

  /** Header string */
  private static final String HEADER_CLASS_RESTRICTION_EXTENDING = "extends ";

  /** Header string */
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
    if(args.length > 2 && !args[2].endsWith(".md")) {
      LOG.warning("Third file name doesn't end with .md!");
      System.exit(1);
    }
    if(args.length > 3 && !args[3].endsWith(".md")) {
      LOG.warning("Fourth file name doesn't end with .md!");
      System.exit(1);
    }
    Path byclsname = Paths.get(args[0]);
    Path byoptname = Paths.get(args[1]);
    Path byclsnamew = args.length >= 3 ? Paths.get(args[2]) : null;
    Path byoptnamew = args.length >= 4 ? Paths.get(args[3]) : null;

    try {
      createDirectories(byclsname.getParent());
      createDirectories(byoptname.getParent());
      createDirectories(byclsnamew != null ? byclsnamew.getParent() : null);
      createDirectories(byoptnamew != null ? byoptnamew.getParent() : null);
    }
    catch(IOException e) {
      LOG.exception(e);
      System.exit(1);
    }

    Map<Class<?>, List<Parameter<?>>> byclass = new HashMap<>();
    Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt = new HashMap<>();
    try {
      buildParameterIndex(byclass, byopt);
    }
    catch(Exception e) {
      LOG.exception(e);
      System.exit(1);
    }

    try (OutputStream byclassstream = Files.newOutputStream(byclsname)) {
      makeByClassOverview(byclass, new HTMLFormat()).writeTo(byclassstream);
    }
    catch(IOException e) {
      LOG.exception("IO Exception writing output.", e);
      System.exit(1);
    }
    if(byclsnamew != null) {
      try (OutputStream byclassfo = Files.newOutputStream(byclsnamew); //
          MarkdownDocStream byclassstream = new MarkdownDocStream(byclassfo)) {
        makeByClassOverview(byclass, new MarkdownFormat(byclassstream));
      }
      catch(IOException e) {
        LOG.exception("IO Exception writing output.", e);
        System.exit(1);
      }
    }

    try (OutputStream byoptstream = Files.newOutputStream(byoptname)) {
      makeByOptOverview(byopt, new HTMLFormat()).writeTo(byoptstream);
    }
    catch(IOException e) {
      LOG.exception("IO Exception writing output.", e);
      System.exit(1);
    }

    if(byoptnamew != null) {
      try (OutputStream byoptfo = Files.newOutputStream(byoptnamew); //
          MarkdownDocStream byoptstream = new MarkdownDocStream(byoptfo)) {
        makeByOptOverview(byopt, new MarkdownFormat(byoptstream));
      }
      catch(IOException e) {
        LOG.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }
    // Forcibly terminate, as some class may have spawned a thread.
    System.exit(0);
  }

  /**
   * Create output directories
   * 
   * @param parent Parent folder
   * @throws IOException on IO errors
   */
  private static void createDirectories(Path parent) throws IOException {
    if(parent != null) {
      Files.createDirectories(parent);
    }
  }

  /**
   * Build the parameter index maps.
   * 
   * @param byclass Index by class
   * @param byopt Index by option
   */
  private static void buildParameterIndex(Map<Class<?>, List<Parameter<?>>> byclass, Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt) {
    final ArrayList<TrackedParameter> options = new ArrayList<>();
    ExecutorService es = Executors.newSingleThreadExecutor();
    Class<?> appc = appBaseClass();
    for(final Class<?> cls : sorted(ELKIServiceRegistry.findAllImplementations(Object.class, false), ELKIServiceScanner.SORT_BY_NAME)) {
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
      return Class.forName("elki.application.AbstractApplication");
    }
    catch(ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Get the constructor of a parameterization class.
   * 
   * @param cls Class
   * @return Constructor
   */
  protected static Constructor<?> getConstructor(final Class<?> cls) {
    try {
      return cls.getConstructor(Parameterization.class);
    }
    catch(java.lang.NoClassDefFoundError e) {
      return null;
    }
    catch(Exception | java.lang.Error e) {
      // Not parameterizable.
      LOG.warning(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Helper class to instantiate a class to get its parameters.
   *
   * @author Erich Schubert
   */
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

  /**
   * Output format abstraction.
   *
   * @author Erich Schubert
   *
   * @hidden
   * @param <T> State type (e.g., HTML Elements for HTML output)
   */
  private interface Format<T> {
    /**
     * Initialize
     * 
     * @param title Title string
     */
    void init(String title);

    /**
     * Begin a new definition list
     * @return State (e.g., container element)
      */
    T topDList();

    /**
     * Make a new unordered list
     * 
     * @param parent Parent state (e.g., element)
     * @param header Header string
     * @return State (e.g., container element)
     */
    T makeUList(T parent, String header);

    /**
     * Write a class definition.
     * 
     * @param parent Parent state (e.g., element)
     * @param cls Class to write
     * @return State (e.g., container element)
     */
    T writeClassD(T parent, Class<?> cls);

    /**
     * Write a class entry.
     * 
     * @param parent Parent state (e.g., element)
     * @param cls Class to write
     * @return State (e.g., container element)
     */
    T writeClassU(T parent, Class<?> cls);

    /**
     * Write an option definition.
     * 
     * @param parent Parent state (e.g., element)
     * @param opt First option
     * @return State (e.g., container element)
     */
    T writeOptionD(T parent, Parameter<?> opt);

    /**
     * Write an option entry.
     * 
     * @param parent Parent state (e.g., element)
     * @param opt First option
     * @return State (e.g., container element)
     */
    T writeOptionU(T parent, Parameter<?> opt);

    /**
     * Append a class restriction.
     * 
     * @param elemdd Container element
     * @param restriction Class restriction
     */
    void appendClassRestriction(T elemdd, Class<?> restriction);

    /**
     * Append known implementations (unless empty).
     * 
     * @param elemdd Container element.
     * @param restriction Class restriction
     */
    void appendKnownImplementationsIfNonempty(T elemdd, Class<?> restriction);

    /**
     * Append string containing the default value.
     *
     * @param optdd HTML Element
     * @param par Parameter
     */
    void appendDefaultValueIfSet(T optdd, Parameter<?> par);
  }

  /**
   * HTML output format.
   *
   * @author Erich Schubert
   */
  private static class HTMLFormat implements Format<Element> {
    /**
     * Base class for linking
     */
    Class<?> base = getBaseClass();

    /**
     * Stylesheet file
     */
    private static final String CSSFILE = "stylesheet.css";

    /**
     * Header string
     */
    private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

    /**
     * Main HTML document
     */
    Document htmldoc;

    /**
     * Main DL element containing the definition table
     */
    Element maindl;

    /**
     * HTML output format
     * 
     * @throws IOException on configuration errors
     */
    HTMLFormat() throws IOException {
      DocumentBuilder builder;
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
      }
      catch(ParserConfigurationException e) {
        throw new IOException(e);
      }
      DOMImplementation impl = builder.getDOMImplementation();
      htmldoc = impl.createDocument(HTMLUtil.HTML_NAMESPACE, HTMLUtil.HTML_HTML_TAG, null);
    }

    @Override
    public void init(String title) {
      assert !htmldoc.getDocumentElement().hasChildNodes();
      // head
      Element head = htmldoc.createElement(HTMLUtil.HTML_HEAD_TAG);
      head.appendChild(htmldoc.createComment(MODIFICATION_WARNING));
      htmldoc.getDocumentElement().appendChild(head);
      // meta with charset information
      Element meta = htmldoc.createElement(HTMLUtil.HTML_META_TAG);
      meta.setAttribute(HTMLUtil.HTML_HTTP_EQUIV_ATTRIBUTE, HTMLUtil.HTML_HTTP_EQUIV_CONTENT_TYPE);
      meta.setAttribute(HTMLUtil.HTML_CONTENT_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_HTML_UTF8);
      head.appendChild(meta);
      // stylesheet
      Element css = htmldoc.createElement(HTMLUtil.HTML_LINK_TAG);
      css.setAttribute(HTMLUtil.HTML_REL_ATTRIBUTE, HTMLUtil.HTML_REL_STYLESHEET);
      css.setAttribute(HTMLUtil.HTML_TYPE_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_CSS);
      css.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, CSSFILE);
      head.appendChild(css);
      // title
      head.appendChild(htmldoc.createElement(HTMLUtil.HTML_TITLE_TAG)).appendChild(htmldoc.createTextNode(title));
      // body
      Element body = htmldoc.createElement(HTMLUtil.HTML_BODY_TAG);
      htmldoc.getDocumentElement().appendChild(body)//
          .appendChild(htmldoc.createComment(MODIFICATION_WARNING));
      body.appendChild(htmldoc.createElement(HTMLUtil.HTML_H1_TAG)).appendChild(htmldoc.createTextNode(title + ":"));
      // Main definition list
      maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
      body.appendChild(maindl);
    }

    /**
     * Write to an output stream
     * 
     * @param refstream Output stream to write to
     * @throws IOException on I/O error
     */
    public void writeTo(OutputStream refstream) throws IOException {
      HTMLUtil.writeXHTML(htmldoc, refstream);
    }

    @Override
    public Element topDList() {
      return maindl;
    }

    @Override
    public Element makeUList(Element parent, String header) {
      if(header != null && !header.isEmpty()) {
        parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_P_TAG)).appendChild(htmldoc.createTextNode(header));
      }
      return (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_UL_TAG));
    }

    @Override
    public Element writeClassD(Element parent, Class<?> cls) {
      assert (HTMLUtil.HTML_DL_TAG.equals(parent.getTagName()));
      // DT = definition term
      Element classdt = (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_DT_TAG));
      classdt.appendChild(linkForClassName(cls, base));

      // DD = definition description
      return (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_DD_TAG));
    }

    @Override
    public Element writeClassU(Element parent, Class<?> cls) {
      assert (HTMLUtil.HTML_UL_TAG.equals(parent.getTagName()));
      Element classli = (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_LI_TAG));
      classli.appendChild(linkForClassName(cls, base));
      return classli;
    }

    @Override
    public Element writeOptionD(Element parent, Parameter<?> firstopt) {
      assert HTMLUtil.HTML_DL_TAG.equals(parent.getTagName());
      // DT = definition term
      Element optdt = (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_DT_TAG));
      // Anchor for references
      optdt.setAttribute(HTMLUtil.HTML_ID_ATTRIBUTE, firstopt.getOptionID().getName());
      // option name
      optdt.appendChild(htmldoc.createElement(HTMLUtil.HTML_TT_TAG)).appendChild(htmldoc.createTextNode(//
          SerializedParameterization.OPTION_PREFIX + firstopt.getOptionID().getName() + " " + firstopt.getSyntax()));
      // DD = definition description
      Element optdd = (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_DD_TAG));
      optdd.appendChild(HTMLUtil.appendMultilineText(htmldoc, //
          htmldoc.createElement(HTMLUtil.HTML_P_TAG), firstopt.getOptionID().getDescription()));
      return optdd;
    }

    @Override
    public Element writeOptionU(Element parent, Parameter<?> firstopt) {
      assert HTMLUtil.HTML_UL_TAG.equals(parent.getTagName());
      Element optli = (Element) parent.appendChild(htmldoc.createElement(HTMLUtil.HTML_LI_TAG));
      // option name
      optli.appendChild(htmldoc.createElement(HTMLUtil.HTML_TT_TAG)).appendChild(htmldoc.createTextNode(//
          SerializedParameterization.OPTION_PREFIX + firstopt.getOptionID().getName() + " " + firstopt.getSyntax()));
      // description
      optli.appendChild(HTMLUtil.appendMultilineText(htmldoc, //
          htmldoc.createElement(HTMLUtil.HTML_P_TAG), firstopt.getOptionID().getDescription()));
      // class restriction?
      // was: using getRestrictionClass(oid, firstopt, byopt);
      Class<?> superclass = getRestrictionClass(firstopt);
      appendClassRestriction(optli, superclass);
      // default value?
      appendDefaultValueIfSet(optli, firstopt);
      // known values?
      appendKnownImplementationsIfNonempty(optli, superclass);
      return optli;
    }

    @Override
    public void appendClassRestriction(Element elemdd, Class<?> restriction) {
      if(restriction == null) {
        // Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        // p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
        // p.appendChild(htmldoc.createTextNode(NO_CLASS_RESTRICTION));
        // elemdd.appendChild(p);
        return;
      }
      Element p = (Element) elemdd.appendChild(htmldoc.createElement(HTMLUtil.HTML_P_TAG));
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
      p.appendChild(htmldoc.createTextNode(restriction.isInterface() ? HEADER_CLASS_RESTRICTION_IMPLEMENTING : HEADER_CLASS_RESTRICTION_EXTENDING));
      p.appendChild(linkForClassName(restriction, base));
    }

    @Override
    public void appendKnownImplementationsIfNonempty(Element elemdd, Class<?> restriction) {
      if(restriction == null || restriction == Object.class) {
        return;
      }
      List<Class<?>> implementations = ELKIServiceRegistry.findAllImplementations(restriction);
      if(implementations.isEmpty()) {
        return;
      }
      elemdd.appendChild(htmldoc.createElement(HTMLUtil.HTML_P_TAG))//
          .appendChild(htmldoc.createTextNode(HEADER_KNOWN_IMPLEMENTATIONS));
      Element ul = (Element) elemdd.appendChild(htmldoc.createElement(HTMLUtil.HTML_UL_TAG));
      for(Class<?> c : sorted(implementations, ELKIServiceScanner.SORT_BY_NAME)) {
        ul.appendChild(htmldoc.createElement(HTMLUtil.HTML_LI_TAG))//
            .appendChild(linkForClassName(c, restriction));
      }
    }

    @Override
    public void appendDefaultValueIfSet(Element optdd, Parameter<?> par) {
      if(!par.hasDefaultValue()) {
        // Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        // p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
        // p.appendChild(htmldoc.createTextNode(NO_DEFAULT_VALUE));
        // optdd.appendChild(p);
        return;
      }
      Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
      p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
      if(par instanceof ClassParameter<?>) {
        final ClassParameter<?> cpar = (ClassParameter<?>) par;
        p.appendChild(linkForClassName(cpar.getDefaultValue(), cpar.getRestrictionClass()));
      }
      else if(par instanceof RandomParameter) {
        p.appendChild(htmldoc.createTextNode(par.getDefaultValue() == RandomFactory.DEFAULT //
            ? "use global random seed" : par.getDefaultValueAsString()));
      }
      else {
        p.appendChild(htmldoc.createTextNode(par.getDefaultValueAsString()));
      }
      optdd.appendChild(p);
    }

    /**
     * Make a link for a class name
     * 
     * @param cls Class name
     * @param ref Reference class for name shortening
     * @return HTML element
     */
    private Element linkForClassName(Class<?> cls, Class<?> ref) {
      Element a = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
      a.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, cls.getName().replace('.', '/') + ".html");
      a.appendChild(htmldoc.createTextNode(ClassParameter.canonicalClassName(cls, ref)));
      return a;
    }
  }

  /**
   * Make the by-class overview
   * 
   * @param <T> Formatter state
   * @param <F> Formatter type
   * @param byclass By-class map
   * @param format Formatter
   * @return Formatter
   */
  private static <T, F extends Format<T>> F makeByClassOverview(Map<Class<?>, List<Parameter<?>>> byclass, F format) {
    format.init("ELKI command line parameter overview");
    for(Class<?> cls : sorted(byclass.keySet(), ELKIServiceScanner.SORT_BY_NAME)) {
      T classdd = format.writeClassD(format.topDList(), cls);
      T classdl = format.makeUList(classdd, null);
      for(Parameter<?> opt : byclass.get(cls)) {
        format.writeOptionU(classdl, opt);
      }
    }
    return format;
  }

  /**
   * Make the by-option overview
   * 
   * @param <T> Formatter state
   * @param <F> Formatter type
   * @param byopt By option map
   * @param format Formatter
   * @return Formatter
   */
  private static <T, F extends Format<T>> F makeByOptOverview(Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt, F format) {
    format.init("ELKI command line parameter overview by option");
    for(OptionID oid : sorted(byopt.keySet(), SORT_BY_OPTIONID)) {
      Parameter<?> firstopt = byopt.get(oid).get(0).getFirst();
      T optdl = format.writeOptionD(format.topDList(), firstopt);
      // class restriction?
      Class<?> superclass = getRestrictionClass(oid, firstopt, byopt);
      format.appendClassRestriction(optdl, superclass);
      // default value
      format.appendDefaultValueIfSet(optdl, firstopt);
      // known values?
      format.appendKnownImplementationsIfNonempty(optdl, superclass);
      // nested definition list for options
      T classesul = format.makeUList(optdl, HEADER_PARAMETER_FOR);
      for(Pair<Parameter<?>, Class<?>> clinst : sorted(byopt.get(oid), SORT_BY_OPTIONID_PRIORITY)) {
        T classli = format.writeClassU(classesul, clinst.getSecond());
        Class<?> ocls = getRestrictionClass(clinst.getFirst());
        // TODO: re-add back reporting of *removed* class restrictions.
        if(ocls != null && !ocls.equals(superclass)) {
          format.appendClassRestriction(classli, ocls);
        }
        Parameter<?> param = clinst.getFirst();
        // FIXME: re-add back if a subtype removes the default value
        if(param.getDefaultValue() != null && !param.getDefaultValue().equals(firstopt.getDefaultValue())) {
          format.appendDefaultValueIfSet(classli, param);
        }
      }
    }
    return format;
  }

  /**
   * Markdown output format
   */
  private static class MarkdownFormat implements Format<Void> {
    /**
     * Base class for linking
     */
    Class<?> base = getBaseClass();

    /**
     * Markdown output stream
     */
    private MarkdownDocStream out;

    /**
     * Constructor.
     * 
     * @param out Markdown output stream
     */
    public MarkdownFormat(MarkdownDocStream out) {
      this.out = out;
    }

    @Override
    public void init(String title) {
      out.append("# ").append(title).par();
    }

    @Override
    public Void topDList() {
      return null;
    }

    @Override
    public Void makeUList(Void parent, String header) {
      if(header != null && !header.isEmpty()) {
        out.indent(0).par().append(header).nl();
      }
      return null;
    }

    @Override
    public Void writeClassD(Void parent, Class<?> cls) {
      javadocLink(out.par().indent(0), cls, base, "`").append(':').par();
      return null;
    }

    @Override
    public Void writeClassU(Void parent, Class<?> cls) {
      javadocLink(out.indent(0).append("- "), cls, base, "").lf().indent(2);
      return null;
    }

    @Override
    public Void writeOptionD(Void parent, Parameter<?> firstopt) {
      out.par().indent(0).append("`").append(SerializedParameterization.OPTION_PREFIX) //
          .append(firstopt.getOptionID().getName()).append(' ').append(firstopt.getSyntax()).append("`: ").par()//
          .append(firstopt.getOptionID().getDescription()).par();
      return null;
    }

    @Override
    public Void writeOptionU(Void parent, Parameter<?> opt) {
      out.indent(0).append("- `").append(SerializedParameterization.OPTION_PREFIX) //
          .append(opt.getOptionID().getName()).append(' ').append(opt.getSyntax()) //
          .append("`").par().indent(2);
      if(opt.getOptionID().getDescription() != null) {
        out.append(opt.getOptionID().getDescription()).lf();
      }
      // class restriction?
      appendClassRestriction(parent, getRestrictionClass(opt));
      // default value
      appendDefaultValueIfSet(parent, opt);
      // known values?
      if(FULL_WIKI_OUTPUT) {
        appendKnownImplementationsIfNonempty(parent, getRestrictionClass(opt));
      }
      return null;
    }

    @Override
    public void appendClassRestriction(Void elemdd, Class<?> restriction) {
      if(restriction == null || restriction == Object.class) {
        // out.append(HEADER_CLASS_RESTRICTION).append(NO_CLASS_RESTRICTION).lf();
        return;
      }
      javadocLink(out.lf().append(HEADER_CLASS_RESTRICTION) //
          .append(restriction.isInterface() ? HEADER_CLASS_RESTRICTION_IMPLEMENTING : HEADER_CLASS_RESTRICTION_EXTENDING), //
          restriction, base, "").lf();
    }

    @Override
    public void appendKnownImplementationsIfNonempty(Void elemdd, Class<?> restriction) {
      if(restriction == null || restriction == Object.class) {
        return;
      }
      List<Class<?>> implementations = ELKIServiceRegistry.findAllImplementations(restriction);
      if(implementations.isEmpty()) {
        return;
      }
      out.lf().append(HEADER_KNOWN_IMPLEMENTATIONS).nl();
      for(Class<?> c : sorted(implementations, ELKIServiceScanner.SORT_BY_NAME)) {
        javadocLink(out.append("- "), c, restriction, "").nl();
      }
    }

    @Override
    public void appendDefaultValueIfSet(Void optdd, Parameter<?> par) {
      if(!par.hasDefaultValue()) {
        // out.append(HEADER_DEFAULT_VALUE).append(NO_DEFAULT_VALUE).lf();
        return;
      }
      out.lf().append(HEADER_DEFAULT_VALUE);
      if(par instanceof ClassParameter<?>) {
        final ClassParameter<?> cpar = (ClassParameter<?>) par;
        javadocLink(out, cpar.getDefaultValue(), cpar.getRestrictionClass(), "").lf();
      }
      else if(par instanceof RandomParameter) {
        out.append(par.getDefaultValue() == RandomFactory.DEFAULT //
            ? "use global random seed" : par.getDefaultValueAsString()).lf();
      }
      else {
        out.append(par.getDefaultValueAsString()).lf();
      }
    }

    /**
     * Generate a JavaDoc link
     *
     * @param out Output stream
     * @param cls Class
     * @param base Class for simplification
     * @param wrap Characters to wrap around the link
     * @return {@code this}
     */
    MarkdownDocStream javadocLink(MarkdownDocStream out, Class<?> cls, Class<?> base, String wrap) {
      wrap = wrap == null ? "" : wrap;
      return out.append('[').append(wrap).append(ClassParameter.canonicalClassName(cls, base)).append(wrap) //
          .append("](./releases/current/doc/").append(cls.getName().replace('.', '/')).append(".html)");
    }
  }

  /**
   * Get the restriction class from an option
   * 
   * @param opt Option parameter
   * @return restriction class
   */
  private static Class<?> getRestrictionClass(Parameter<?> opt) {
    return opt instanceof ClassParameter ? ((ClassParameter<?>) opt).getRestrictionClass() //
        : opt instanceof ClassListParameter ? ((ClassListParameter<?>) opt).getRestrictionClass() //
            : null;
  }

  /**
   * Get the restriction class of an option.
   *
   * @param oid Option ID
   * @param firstopt Parameter
   * @param byopt Option to parameter map
   * @return Restriction class
   */
  private static Class<?> getRestrictionClass(OptionID oid, final Parameter<?> firstopt, Map<OptionID, List<Pair<Parameter<?>, Class<?>>>> byopt) {
    Class<?> superclass = getRestrictionClass(firstopt);
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

  /**
   * Get the base class, for naming.
   *
   * @return Base class.
   */
  private static Class<?> getBaseClass() {
    try {
      return Class.forName("elki.KDDTask");
    }
    catch(ClassNotFoundException e) {
      return null; // Just worse links, not serious.
    }
  }

  /**
   * Sort a collection of classes.
   *
   * @param <T> Object type
   * @param cls Classes to sort
   * @param c Comparator
   * @return Sorted list
   */
  private static <T> ArrayList<T> sorted(Collection<T> cls, Comparator<? super T> c) {
    ArrayList<T> sorted = new ArrayList<>(cls);
    sorted.sort(c);
    return sorted;
  }

  /**
   * Sort parameters by their option id.
   */
  private static final Comparator<OptionID> SORT_BY_OPTIONID = new Comparator<OptionID>() {
    @Override
    public int compare(OptionID o1, OptionID o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  };

  /**
   * Sort parameters by OptionID, then class priority.
   */
  private static final Comparator<Pair<Parameter<?>, Class<?>>> SORT_BY_OPTIONID_PRIORITY = new Comparator<Pair<Parameter<?>, Class<?>>>() {
    @Override
    public int compare(Pair<Parameter<?>, Class<?>> o1, Pair<Parameter<?>, Class<?>> o2) {
      int c = SORT_BY_OPTIONID.compare(o1.first.getOptionID(), o2.first.getOptionID());
      return (c != 0) ? c : ELKIServiceScanner.SORT_BY_PRIORITY.compare(o1.second, o2.second);
    }
  };
}
