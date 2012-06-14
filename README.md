# D2RQ – A Database to RDF Mapper

D2RQ exposes the contents of relational databases as RDF. It consists of:

* The **D2RQ Mapping Language**. Use it to write mappings between database tables and RDF vocabularies or OWL ontologies.
* The **D2RQ Engine**, a SPARQL-to-SQL rewriter that can evaluate SPARQL queries over your mapped database. It extends ARQ, the query engine that is part of Apache Jena.
* **D2R Server**, a web application that provides access to the database via the SPARQL Protocol, as Linked Data, and via a simple HTML interface.

## Homepage and Documentation

Learn more about D2RQ at its homepage: http://d2rq.org/

## License

Apache License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.html

## Contact, feedback, discussion

The project's mailing list is here:
https://lists.sourceforge.net/lists/listinfo/d2rq-map-devel

Also check the open issues here on GitHub for feature/bug discussion.

## Building from source

### Prerequisites

You need some tools in order to be able to build D2RQ. Depending on your operating system, they may or may not be already installed.

* [git](http://git-scm.com/), for forking the source code repository from GitHub. Run `git` on the command line to see if it's there.
* [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) v5 or later, for compiling Java sources. Run `java -version` and `javac` on the command line to see if it's there.
* [Apache Ant](http://ant.apache.org/), for building D2RQ. Run `ant` on the command line to see if it's there.

### Getting the source

Get the code by forking the GitHub repository and cloning your fork, or directly clone the main repository:

```git clone git@github.com:d2rq/d2rq.git```

### Doing Ant builds

D2RQ uses Apache Ant as its build system. You can run `ant -p` from the project's main directory to get an overview of available targets:

To run the D2RQ tools, you need to do at least `ant jar`.

<table>
<tr><td>ant all</td><td>Generate distribution files in zip and tar.gz formats</td></tr>
<tr><td>ant clean</td><td>Deletes all generated artefacts</td></tr>
<tr><td>ant compile</td><td>Compile project classes</td></tr>
<tr><td>ant compile.tests</td><td>Compile test classes</td></tr>
<tr><td>ant jar</td><td>Generate project jar file</td></tr>
<tr><td>ant javadoc</td><td>Generate Javadoc API documentation</td></tr>
<tr><td>ant tar</td><td>Generate distribution file in tar.gz format</td></tr>
<tr><td>ant test</td><td>Run tests</td></tr>
<tr><td>ant vocab.config</td><td>Regenerate Config vocabulary files from Turtle source</td></tr>
<tr><td>ant vocab.d2rq</td><td>Regenerate D2RQ vocabulary files from Turtle source</td></tr>
<tr><td>ant war</td><td>Generate war archive for deployment in servlet container</td></tr>
<tr><td>ant zip</td><td>Generate distribution file in zip format</td></tr>
</table>

## Running D2RQ

After building with `ant jar`, you can test-run the various components. Let's assume you have a MySQL database called `mydb` on your machine.

### Generating a default mapping file

```./generate-mapping -u root -o mydb.ttl jdbc:mysql:///mydb```

This generates a mapping file `mydb.ttl` for your database.

### Dumping the database

```./dump-rdf -m mydb.ttl -o dump.nt```

This creates `dump.nt`, a dump containing the mapped RDF in N-Triples format.

### Running D2R Server

```./d2r-server mydb.ttl```

This starts up a server at http://localhost:2020/

### Deploying D2R Server into a servlet container

Edit `/webapp/WEB-INF/web.xml` to point the `configFile` parameter to the location of your mapping file.

Build a war file with `ant war`.

Deploy the war file, e.g., by copying it into the servlet container's `webapps` directory.

### Running the unit tests

The unit tests can be executed with `ant test`.

Some unit tests rely on MySQL being present, and require that two databases are created:

1. A database called `iswc` that contains the data from `/doc/example/iswc-mysql.sql`:

    echo "CREATE DATABASE iswc" | mysql -u root
    mysql -u root iswc < doc/example/iswc-mysql.sql

2. An empty database called `D2RQ_TEST`.
