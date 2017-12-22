## The Grinder

Forked from [Sourceforge](https://sourceforge.net/p/grinder/) to try
wrapping the JARs into a
[Leiningen](https://github.com/technomancy/leiningen).  Leiningen
handles dependencies, class path, and Uberjar build for you.

The Author
[advises](https://sourceforge.net/p/grinder/mailman/message/31038768/)
to skip the tests initially

    mvn -DskipTests package

> The HTTP module re-uses test utilities belonging to the core
> module. Try using -DskipTests instead.  This will compile the tests,
> but not execute them.  See [dead
> link](http://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-test.html).

    mvn -DskipTests install

With grider JAR installed into your local Maven directory ~/.m2/ you
should be able to

    cd grinder-clj
    lein deps
    lein deps :tree

and Leiningen will find the artifacts.  Then build and run the
Leiningen project.  For details see the Clojure
[project](./grinder-clj).
