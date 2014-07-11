# ComportexViz

A web-based visualization layer for
[Comportex](http://github.com/floybix/comportex/).

See it in action in (this blog
post)[http://floybix.github.io/2014/07/11/visualization-driven-development-of-the-cortical-learning-algorithm/].


## Usage

Get [Leiningen](http://leiningen.org/) first.

Use git to clone [Comportex](http://github.com/floybix/comportex/), and:

```
lein do cljx once, check, install
```

Then use git to clone this repository, and:

```
lein cljsbuild clean
lein cljsbuild once
```

Now open `public/*.html` in a web browser, preferably Google
Chrome. Each HTML page loads the corresponding input generator defined
in `src/comportexviz/demos/`.


## License

Copyright © 2014 Felix Andrews

Distributed under your choice of
* the Eclipse Public License, the same as Clojure.
* the GNU Public Licence, Version 3 http://www.gnu.org/licenses/gpl.html, the same as NuPIC.
