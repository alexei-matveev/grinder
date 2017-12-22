## The Grinder: Clojure Leiningen Project

A Clojure library designed to handle grinder build, class path, and
uberjar build.

## Usage

    lein run Console [-headless]
    lein run Grinder

Console needs to be able to listen to a few ports. The Console listens
by default to 0.0.0.0:6372, this is where the agents will need to
[connect
to](http://grinder.sourceforge.net/g3/getting-started.html). At :6373
there is a REST endpoint of the [Console
service](http://grinder.sourceforge.net/g3/console-service.html) which
is, BTW, also a Clojue [subproject](../grinder-console-service).

FIXME:  On a  headless Linux  VM  `lein run  Console` shows  exception
saying  DISPLAY  is not  set  while  `lein -jar  ${uber}.jar  Console`
continues to start the console service as if one specified `-headless`
option. Same is observed behaviour if you launch the class directly:

    java -classpath ${uber}.jar net.grinder.Console

On the other hand if you start it like this:

    java -classpath $(lein classpath) grinder_clj.core Console

you will  get the "No X11  DISPLAY variable was set"  error.  There is
some  non-trivial logic  in ConsoleFoundation.java  that "dynamically"
loads UI versions and if nothing works falls back to TextUI.

## License

Copyright © 2017 Alexei Matveev <alexei.matveev@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
