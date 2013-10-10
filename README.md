Sclastic
========
Scala compiler kit for estimating the complexity of Scala source.

Author: Ron Coleman

About
=====
Sclastic is an experimental kit
for estimating the cyclomatic complexity (M) and lines of code (LOC)
of Scala source. The core is focused on processing a portfolio of Scala projects compressed in .zip formats
downloaded from GitHub, Inc.

Theory of operation
-------------------
Sclastic estimates, M, the [cyclomatic complexity](http://www.literateprogramming.com/mccabe.pdf) given as

    M = pi + 1

where pi is the number of decision points of a method.

To estimate pi, Sclastic counts "hard" and "soft" decision points (dps). The hard dps are if, while, case-match, etc.
statements.

The signatures of soft dps are "predicate contexts," that is, higher-order functions that take
Boolean returning function objects, be they named or anonymous (i.e., lambdas).
Higher-order functions like filter, count, compare, etc. are some examples
of predicate contexts
The challenge is the signatures of predicate contexts, practically speaking, can only be "learned" from the input.
Sclastic stores the signatures that it discovers in a database, i.e., the "book," which is then consulted
when counting soft dps.

It may be necessary to estimate the
probability that the signatures of some predicate are not in the book (i.e., the "soft miss" rate)
since references to imports may not
be in the portfolio of projects under analysis.
To estimate the miss soft rate we have

    P(soft miss) = k x w

where k is the miss package rate and w is the probability a project contains packages with predicate contexts.

Project artifacts
-----------------
Sclastic is separated into several artifacts:

* *scaly.sclastic.Sclastic.*
	This is a convenience class. It automates running the different phases of the compiler and retuns
	a list of descriptors. Each descriptor is just the path of the .scala file, the package name containing
	the class, the name of the class, the name of the method in the class, the LOC, and the M for the method.
	This is the simplest and easiest way to use Sclastic. One overloaded method takes the name of the raw .scala file
	and the other overloaded method takes a list of strings with comments and empty lines removed.
* *scaly.sclastic.Runner.*
	The core program, Runner, uses the Sclastic object to do all its processing then adds more of it own for streaming
	a large collection of GitHuib projects. It is driven by a confguration file.
	This configuration file points to the root directory of the input .zip files which Runner decompresses on-the-fly.
	The configuration file also points to the working directory for output files, and it has some switches for toggling
	a couple of options. One importnt option, "lint," tells Runner to compile the database of predicate
	contexts, that is, higher-order functions that take Boolean returning function objects. 
	Another independent option, "hofdb," points to the database of predicate contexts. If this key is missing in the
	config file, Runner defaults to the List API hofs.
* *scaly.sclastic.BackScatter.*
	This program consumes the main report output by Runner and builds a "backscatter" plot of M vs. LOC as an PNG file.

* *scaly.sclastic.ImportLocator.*
	This program identifies missing imports in the portfolio of Scala projects for estimating
	the the "soft miss" rate. By getting the uniquely missing imports, we can calculate the missing
	package rate, k, above. The parameter, w, is estimated from the frequency of projects with packages with
	predicate contexts which Runner provides as a report.
* *scaly.sclastic.test.*
	These programs are JUnit 4 test programs run by ScalaTest. Most of these programs have input files named
	.scala_ in the test-files project folder.

Configuration files
-------------------
Sclastic is driven by configuration files that have key-value format. The keys are

* *root*
	The root folder that Runner uses to look for project .zip files.
* *report*
	The path of the main report file that Runner outputs.
* *workdir*
	The path of the working directory that Runner uses to output other report files. In theory, this
	folder is the prefix of the report file.
* *hofdb*
	The full path of the soft signature database of predicate context.
* *lint*
	Controls whether Runner will generate its HOF database. The value is "true" or otherwise.
* *plot*
	The full path of the BackScatter PNG file.
* *imports*
	The full path of the report file of declared imports found in the list of projects.
* *pkgs*
	The full path of the report file of declared packages found the in the list of projects.
* *structs*
	The name of the report file of the declared classes, traits, or objects in the list of projects.
