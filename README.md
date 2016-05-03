# ELKI
##### Environment for Developing KDD-Applications Supported by Index-Structures
[![DBLP:journals/pvldb/SchubertKEZSZ15](https://img.shields.io/badge/DBLP--BibTeX-journals%2Fpvldb%2FSchubertKEZSZ15-brightgreen.svg)](http://dblp.uni-trier.de/rec/bibtex/journals/pvldb/SchubertKEZSZ15)
[![License AGPL-3.0](https://img.shields.io/badge/License-AGPL--3-brightgreen.svg)](http://elki.dbs.ifi.lmu.de/wiki/License)
[![Build Status](https://travis-ci.org/elki-project/elki.svg?branch=master)](https://travis-ci.org/elki-project/elki)

## Quick Summary
ELKI is an open source (AGPLv3) data mining software written in Java. The focus of ELKI is research in algorithms, with an emphasis on unsupervised methods in cluster analysis and outlier detection.
In order to achieve high performance and scalability, ELKI offers many data index structures such as the R*-tree that can provide major performance gains.
ELKI is designed to be easy to extend for researchers and students in this domain, and welcomes contributions in particular of new methods.
ELKI aims at providing a large collection of highly parameterizable algorithms, in order to allow easy and fair evaluation and benchmarking of algorithms. 

## Background

Data mining research leads to many algorithms for similar tasks. A fair and useful comparison of these algorithms is difficult due to several reasons:
 * Implementations of comparison partners are not at hand.
 * If implementations of different authors are provided, an evaluation in terms of efficiency is biased to evaluate the efforts of different authors in efficient programming instead of evaluating algorithmic merits.

On the other hand, efficient data management tools like index-structures can show considerable impact on data mining tasks and are therefore useful for a broad variety of algorithms.

In ELKI, data mining algorithms and data management tasks are separated and allow for an independent evaluation. This separation makes ELKI unique among data mining frameworks like Weka or Rapidminer and frameworks for index structures like GiST. At the same time, ELKI is open to arbitrary data types, distance or similarity measures, or file formats. The fundamental approach is the independence of file parsers or database connections, data types, distances, distance functions, and data mining algorithms. Helper classes, e.g. for algebraic or analytic computations are available for all algorithms on equal terms.


With the development and publication of ELKI, we humbly hope to serve the data mining and database research community beneficially. The framework is **free** for scientific usage ("free" as in "open source", see [License](http://elki.dbs.ifi.lmu.de/wiki/License) for details). In case of application of ELKI in scientific publications, we would appreciate credit in form of a [citation](http://elki.dbs.ifi.lmu.de/wiki/Publications) of the appropriate publication (see [our list of publications](http://elki.dbs.ifi.lmu.de/wiki/Publications)), that is, the publication related to the release of ELKI you were using.

The people behind ELKI are documented on the [Team](http://elki.dbs.ifi.lmu.de/wiki/Team) page.


## The ELKI wiki: Tutorials, HowTos, Documentation

Beginners may want to start at the HowTo documents, [Examples](http://elki.dbs.ifi.lmu.de/wiki/Examples) and [Tutorials](http://elki.dbs.ifi.lmu.de/wiki/Tutorial) to help with difficult configuration scenarios and beginning with ELKI development.

This website serves as community development hub and task tracker for both [bug reports](http://elki.dbs.ifi.lmu.de/report/1), [Tutorials](http://elki.dbs.ifi.lmu.de/wiki/Tutorial), [FAQ](http://elki.dbs.ifi.lmu.de/wiki/FAQ), general issues and development tasks.

The most important documentation pages are: [Tutorial](http://elki.dbs.ifi.lmu.de/wiki/Tutorial), searchable [JavaDoc]((http://elki.dbs.ifi.lmu.de/wiki/JavaDoc)), [FAQ](http://elki.dbs.ifi.lmu.de/wiki/FAQ),
[InputFormat](http://elki.dbs.ifi.lmu.de/wiki/InputFormat), [DataTypes](http://elki.dbs.ifi.lmu.de/wiki/DataTypes), [DistanceFunctions](http://elki.dbs.ifi.lmu.de/wiki/DistanceFunctions), [DataSets](http://elki.dbs.ifi.lmu.de/wiki/DataSets), [Development](http://elki.dbs.ifi.lmu.de/wiki/Development), [Parameterization](http://elki.dbs.ifi.lmu.de/wiki/Parameterization),
[Visualization](http://elki.dbs.ifi.lmu.de/wiki/Visualization), [Benchmarking](http://elki.dbs.ifi.lmu.de/wiki/Benchmarking), and the
list of [Algorithms](http://elki.dbs.ifi.lmu.de/wiki/Algorithms) and [RelatedPublications](http://elki.dbs.ifi.lmu.de/wiki/RelatedPublications).

## Getting ELKI: Download and Citation Policy

You can download ELKI including source code on the [Releases](http://elki.dbs.ifi.lmu.de/wiki/Releases) page.<br /> ELKI uses the [AGPLv3 License](http://elki.dbs.ifi.lmu.de/wiki/License), a well-known open source license.

There is a list of [Publications](http://elki.dbs.ifi.lmu.de/wiki/Publications) that accompany the ELKI releases. When using ELKI in your scientific work, you should cite the publication corresponding to the ELKI release you are using, to give credit. This also helps to improve the repeatability of your experiments. We would also appreciate if you contributed your algorithm to ELKI to allow others to reproduce your results and compare with your algorithm (which in turn will likely get you citations). We try to document every publication used for implementing ELKI: the page [RelatedPublications](http://elki.dbs.ifi.lmu.de/wiki/RelatedPublications) is generated from the source code annotations.

## Efficiency Benchmarking with ELKI

ELKI is quite fast (see [some of our benchmark results](http://elki.dbs.ifi.lmu.de/wiki/Benchmarking)) but the focus lies on a *broad coverage of algorithms and variations*.
We discourage cross-platform benchmarking, because it is easy to produce misleading results by comparing apples and oranges. For fair comparability, you should implement all algorithms within ELKI, and use the same APIs. We have also observed Java JDK versions have a large impact on the runtime performance. To make your results reproducible, please [cite](http://elki.dbs.ifi.lmu.de/wiki/Publications) the version you have been using. See also [Benchmarking](http://elki.dbs.ifi.lmu.de/wiki/Benchmarking).


## Bug Reports and Contact

You can [browse the open bug reports](http://elki.dbs.ifi.lmu.de/report/1) or [create a new bug report](http://elki.dbs.ifi.lmu.de/newticket).

We also appreciate any comments, suggestions and code contributions.<br/> You can contact the core development team by e-mail: `elki () dbs ifi lmu de`

You can also [subscribe to the user mailing list](https://tools.rz.ifi.lmu.de/mailman/listinfo/elki-user) of ELKI, to exchange questions and ideas among other users or to get announcements (e.g., new releases, major changes) by the ELKI team.

Our primary "support" medium is this *community* mailing list. We appreciate if you share experiences and also success stories there that might help other users. This project makes a lot of progress, and information can get outdated rather quickly. If you prefer a web forum, you can *try* asking at [StackOverflow](http://www.stackoverflow.com/), but you should understand that this is a general (and third-party operated) programming community.

## Design Goals

 * Extensibility - ELKI has a very modular design. We want to allow arbitrary combinations of data types, distance functions, algorithms, input formats, index structures and evaluations methods
 * Contributions - ELKI grows only as fast as people contribute. By having a modular design that allows small contributions such as single distance functions and single algorithms, we can have students and external contributors participate in the progress of ELKI
 * Completeness - for an exhaustive comparison of methods, we aim at covering as much published and credited work as we can
 * Fairness - It is easy to do an unfair comparison by badly implementing a competitor. We try to implement every method as good as we can, and by publishing the source code allow for external improvements. We try to add all proposed improvements, such as index structures for faster range and kNN queries
 * Performance - the modular architecture of ELKI allows optimized versions of algorithms and index structures for acceleration
 * Progress - ELKI is changing with every release. To accomodate new features and enhance performance, API breakages are unavoidable. We hope to get a stable API with the 1.0 release, but we are not there yet.

## Building ELKI

ELKI is built using [Maven](https://maven.apache.org/):

    mvn package

will produce an executable `jar` file named `elki/target/elki-<VERSION>.jar`.

To build the SVG based visualization add-on, use

    mvn -P svg package

which produces an executable `jar` file named `addons/batikvis/target/elki-batik-visualization-<VERSION>.jar`

Required dependencies can be found in the folder `dependency` next to the `jar` file.

To build a *standalone jar*, use the `bundle` profile (requires Python for cleaning up the license documentation).
When using IntelliJ, it seems that the best strategy is to run within the
bundle project, as IntelliJ then manages to set up the classpath correctly
(feel free to discuss and suggest better solutions for IntelliJ or NetBeans
support - we are using Eclipse, so we need expert recommendations for
effectively using other IDEs)
