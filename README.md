Sclastic
========
:Info: Scala compiler kit for estimating the complexity of Scala source
:Author: Ron Coleman

About
=====
Sclastic is an experimental kit
for estimating the cyclomatic complexity (M) and lines of code (LOC)
of Scala source. The core is focused on processing a portfolio of Scala projects compressed in .zip formats
downloaded from GitHub, Inc. There are, however, convenience classes for processing individual files.
Even here, though, Sclastic doesn't report M and LOC directly but instead returns a list of "descriptor" objects.

So if you're looking for a quick-and-dirty way to estimate the complexity of Scala source, this is not
necessarily the right place. Yet the tools of the kit are here for such an analysis. You'll just
need to compose them and interpret the output for your particular purposes.

Theory of operation
-------------------

Sclastic estimates, M, the [cyclomatic complexity](http://www.literateprogramming.com/mccabe.pdf) given by'

    M = pi + 1

where pi is the number of decision points of a method.

To estimate pi, Sclastic counts "hard" and "soft" decision points (dps). The hard dps are if, while, case-match, etc.
statements. The signatures of hard dps are knowable in advance since they are "hard-coded" into Scala.

The signatures of soft dps are "predicate contexts," that is, higher-order functions that take
Boolean returning function objects. Higher-order functions like filter, count, compare, etc. are some examples
of predicate contexts
The challenge is the signatures of predicate contexts, practically speaking, can only be "learned" from the input.
Sclastic stores these in a database which is really just a flat file, which is then consulted
when counting for decision points.

Project artifacts
-----------------
Sclastic is separated into several artifacts:

* *Sclastic*
	This is a convenience class. It automates running the different phases of the compiler and retuns
	a list of descriptors. Each descriptor is just the path of the .scala file, the package name containing
	the class, the name of the class, the name of the method in the class, the LOC, and the M for the method.
	This is the simplest and easiest way to use Sclastic. One overloaded method takes the name of the raw .scala file
	and the other overloaded method takes a list of strings with comments and empty lines removed.
* *Runner*
	The core program, Runner, uses the Sclastic object to do all its processing then adds more of it own for streaming
	a large collection of GitHuib projects. It is driven by a confguration file.
	This configuration file points to the root directory of the input .zip files which Runner decompresses on-the-fly.
	The configuration file also points to the working directory for output files, and it has some switches for toggling
	a couple of options. One importnt option, "lint," tells Runner to compile the database of predicate
	contexts, that is, higher-order functions that take Boolean returning function objects. 
	Another independent option, "hofdb," points to the database of predicate contexts. If this key is missing in the
	config file, Runner defaults to the List API hofs.
* *BackScatter*
	This program consumes the main report output by Runner and builds a "backscatter" plot of M vs. LOC as an PNG file.

* *ImportLocator*
	This program identifies missing imports in the portfolio of Scala projects for estimating
	the the "soft miss" rate. However, like BackScatter, this programs depends on the output of Runner.
	Then, it only reports the unique missing imports. Another program, a Korn shell script, misses.ksh,
	computes the "miss package" rate.


 To "best guess" estimate the soft miss rate, you'll
	need to multiply the missing package rate times the ratio of files with predicate contexts.
	There is no script for this but the data is output on Runner's console.
* *config files*
* *Unit test cases and files*
