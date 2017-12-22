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

### Choosing Grinder UI

By default the Grinder Console starts the Swing UI.

On  a headless  Linux VM  `lein  run Console`  shows exception  saying
DISPLAY is  not set while  `lein -jar ${uber}.jar Console`  appears to
start  the console  (rest)  service as  if  one specified  `-headless`
option. Same behaviour is observed if you launch the class directly:

    java -classpath ${uber}.jar net.grinder.Console

On the other hand if you start it like this:

    java -classpath $(lein classpath) net.grinder.Console

or like this

    java -classpath $(lein classpath) grinder_clj.core Console

The  Console would  (try  to)  launch the  Swing  UI.   There is  some
non-trivial logic  in ConsoleFoundation.java that  "dynamically" loads
UI versions and if nothing works falls back to TextUI.

This logic appears to consult the class name in the Java resource
named:

    META-INF/net.grinder.console

and use that as the UI implementation. There appear to be at least two
implementations:

    net.grinder.console.swingui.ConsoleUI
    net.grinder.console.service.Bootstrap

listed respectively in two identically named resource files:

    ../grinder-swing-console/src/main/resources/META-INF/net.grinder.console
    ../grinder-console-service/resources/META-INF/net.grinder.console

The uberjar,  on the other hand,  will only contain one  resource file
named  like  that. It  happens  to  be the  Bootstrap  implementation,
possibly  unfinished.  If  you  edit the  uberjar  with vim  replacing
Bootstrap implementation with  the Swing UI, then  running the uberjar
as above does launch the Swing  UI.  To make that deterministic we put
a third identically named resource into

    ./resources/META-INF/net.grinder.console

choosing the Swing UI option.

## License

Copyright © 2017 Alexei Matveev <alexei.matveev@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
