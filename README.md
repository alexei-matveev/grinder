## The Grinder

Forked from [Sourceforge](https://sourceforge.net/p/grinder/) to try
wrapping the JARs into a
[Leiningen](https://github.com/technomancy/leiningen) project.
Leiningen handles dependencies, class path, and Uberjar build for you.

### Qickstart build instructions

The Author
[advises](https://sourceforge.net/p/grinder/mailman/message/31038768/)
to skip the tests initially

    mvn -DskipTests package

> The HTTP module re-uses test utilities belonging to the core
> module. Try using -DskipTests instead.  This will compile the tests,
> but not execute them.  See [dead
> link](http://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-test.html).

    mvn -DskipTests install

With grinder JAR installed into your local Maven directory ~/.m2/ you
should be able to

    cd grinder-clj
    lein deps

and Leiningen will find the artifacts.  Then build and run the
Leiningen project.  For details see the Clojure
[project](./grinder-clj).

FIXME: there is a ref to a very old version of clojure somewhere. I
get this on the first `lein deps`:

    Retrieving org/clojure/clojure/1.3.0-alpha5/clojure-1.3.0-alpha5.pom from central

Also `lein deps :tree` detects quite a few "possibly confusing
dependencies".
