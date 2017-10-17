# lein-deploy-uberjar

A Leiningen plugin to deploy _uberjars_ (or _fat jars_) to a Maven repository. 

This plugin supports explicitly skipping authentication methods supported natively by Leiningen and delegating authentication to the Amazon Web Services [Default Credential Provider Chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html). This enables deploying uberjars to a private repository hosted on S3, in combination with [this other plugin](https://github.com/s3-wagon-private/s3-wagon-private).

## License

Copyright (C) 2012 Utah Street Labs

Copyright (C) 2017 CSIRO Data61

This version started as one in a series of forks of [the original](https://github.com/utahstreetlabs/lein-deploy-uberjar) by the [Utah Street Labs](https://github.com/utahstreetlabs). None of the intermediate authors added a copyright notice: the code may contain contributions from the GitHub users [Moocar](https://github.com/Moocar), [RallySoftware](https://github.com/RallySoftware) and [likely](https://github.com/likely).

Last but not least, this plugin is based on code originally extracted from [Leiningen](https://leiningen.org/), copyright (C) 2009-2017 Phil Hagelberg, Alex Osborne, Dan Larkin, and contributors.

Distributed under the Eclipse Public License, the same as Clojure. See file [COPYING.txt](COPYING.txt).

## Usage

This is a plugin for Leiningen, an awesome build tool for [Clojure](https://clojure.org/), not an independent program. See the official [Leiningen Tutorial](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md) on how to get started using Leiningen.

### Installation

This plugin is currently not distributed in binary form (soon to come!). To use it you will have to download and build the source code and install the compiled plugin manually in your local maven cache (e.g. `~/.m2/repository` on GNU/Linux systems).

Assuming Leiningen (`lein` command) is already installed in your system, issue the following commands in a terminal:

```bash
git clone https://github.com/ammazza/lein-deploy-uberjar.git
cd lein-deploy-uberjar
lein install
```

Optionally, before `lein install`, you can run the tests. Note that currently the test suite is not comprehensive and only covers functions added since the project was last forked.
```bash
lein test
```

Modify your project's `project.clj` file, adding the plugin to the `:plugins` section:

```clojure
:plugins [[lein-deploy-uberjar/lein-deploy-uberjar "2.1.0"]]
```

### Repositories

Artifact repositories must be specified inside `project.clj`, using either `:repositories` or `:deploy-repositories`. For instance, to specifiy a repository hosted on S3, bucket `my-s3-bucket`:

```clojure
:repositories [["snapshots" {:url "s3p://my-s3-bucket/snapshots" :no-auth true}]
               ["releases" {:url "s3p://my-s3-bucket/releases" :no-auth true}]] 
```

Use of URLs like the above requires the [s3-wagon-private plugin](https://github.com/s3-wagon-private/s3-wagon-private), which must be added to `project.clj` like so:

```clojure
:plugins [[s3-wagon-private "1.3.0"]
          ...] ;; Other plugins
```

### Uberjar deployment

To deploy a project uberjar issue this command

```bash
lein deploy-uberjar [repository]
```

The optional `repository` is the label assigned in the `:repositories` configuration, i.e. the string before the URL (_snapshots_ and _releases_ in the example above). If no repository is specified, the suffix _-SNAPSHOT_ in the version string will be used to identify snapshots.

Note that this plugin appends the qualifier _-stanalone to the uberjars. As a consequence snapshot uberjars will not be timestamped. The idea is that only one uberjar is ever available, with the latest deployment. This behaviour is intentional and limits space consumption on the repository. It may become configurable in the future.



