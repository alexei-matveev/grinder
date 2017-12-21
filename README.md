https://sourceforge.net/p/grinder/mailman/message/31038768/

    mvn -DskipTests package

The HTTP module re-uses test utilities belonging to the core module. Try
using -DskipTests instead. This will compile the tests, but not execute
them. See
http://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-test.html

    mvn -DskipTests install

By now

    cd grinder-clj
    lein deps

should find the grinder artifacts.
