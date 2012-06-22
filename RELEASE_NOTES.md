# D2RQ Release Notes

## D2RQ v0.8.1 - 2012-06-22

This release adds support for W3C's [Direct Mapping of Relational Data to RDF](http://www.w3.org/TR/rdb-direct-mapping/). The new d2r-query script allows SPARQL queries against D2RQ-mapped databases from the command line. There is a comprehensive set of options for including or excluding specific schemas, tables, and columns. The default settings of D2R Server's web interface work better for large databases. D2R Server now serves metadata for each resource, and for the entire dataset, including [VoID](http://www.w3.org/TR/void/) support. Timeouts for SPARQL queries can be specified. And, as usual, many bugfixes and smaller enhancements.

### Features
- support for non-duplicate-preserving W3C Direct Mapping (generate-mapping --w3c) (Luis Eufrasio)
- add command line options to include or exclude specific schemas, tables or columns (Jacobus Geluk)
- added timeouts for SPARQL and page loads: -t, d2r:sparqlTimeout, d2r:pageTimeout
- added d2r-query script to run SPARQL queries against DB from the command line
- VoID support and dataset metadata in D2R Server (Hannes Mühleisen)
- much improved per-resource metadata in D2R Server (Hannes Mühleisen)

### Enhancements
- D2R Server web interface no longer blows up by default because of pages with too much data (#88)
- configurable d2r:limitPerClassMap and d2r:limitPerPropertyBridge to control size of pages in D2R Server web interface (#88)
- made command line tools more consistent (shared parameter lists, error reporting)
- allow running of D2R Server just with just a JDBC connection instead of a mapping file
- allow running of all tools just with -l dump.sql
- dropped -m/-j switches on dump_rdf, just provide naked file/URL
- add --verbose and --debug switches to all command line tools
- use dc:title, dct:title, skos:prefLabel, foaf:name as resource labels if present
- display foaf:img and foaf:depiction as image in resource pages
- allow reading of media type for d2rq:DownloadMap from DB column
- allow class maps without any property bridges nor d2rq:class if they appear in some property bridge's d2rq:refersToClassMap
- use local versions of jQuery and Prototype so that Snorql still works without internet connection
- output warnings when SPARQL joins over different d2rq:translateWith or over d2rq:[uri]Pattern with different column encodings (#22)
- refactoring: made all use of containers type-safe
- refactoring: command line tools, datatype handling, ARQ op-tree rewriting, Jena integration
- improved error reporting for D2R Server configuration files
- use RIOT instead of Jena's old N3 parser for Turtle; improved error reporting for Turtle parse errors
- support PostGIS GEOMETRY datatype
- generate_mapping skips more Oracle system tables and works better for databases with many tables
- generate_mapping puts schema name into default instance IRIs

### Bugfixes
- fix bug where any command line arguments after the 10th are ignored on Windows
- fix bug in SQLScriptLoader where last statement would be ignored if not semicolon-terminated
- bugfix: mapping file cannot be parsed if file extension other than .n3 and .ttl
- fix MySQL bug where column names were treated in a case sensitive way in rare cases
- avoid problem where the d2rq:startupSQLScript is executed twice
- fixed issue #4: DISTINCT with CLOB and BLOB doesn't work on some DBs
- fixed logic bug in isIRI() (Giovanni Mels)
- update links to old web page in D2R Server web interface
- Oracle TIMESTAMP WITH TIME ZONE columns now work
- fix bug where columns AAA.BBB_CCC and AAA_BBB.CCC would cause a propertyBridge name clash in mapping generator
- fix several issues with generate_mapping (crash with unknown datatype in PK; encoding of special chars)
- fix bug where properties with same localName but different namespace wouldn't show in D2R Server (#165)

### Performance
- combine SELECT queries that just differ in their WHERE clauses into a single query
- generate IS NOT NULL constraints in SQL queries (#128)

### Other
- remove Windows service installer; preferred way to run as a service under Windows is to install into Tomcat or Jetty
- deprecated d2rq:allowDistinct as it's no longer needed
- RDF representations of resource descriptions no longer include rdfs:seeAlso links to SPARQL DESCRIBE results for neighbouring resources; it's doubtful whether this ever was useful to anyone


## D2RQ v0.8 - 2012-03-12

New features include preliminary SPARQL 1.1 support, Firebird support, generation of RDFS/OWL schemas for databases, and download maps for making the content of CLOB/BLOB columns accessible via HTTP. Datatype compatibility with Oracle, MySQL, SQL Server and HSQLDB has been greatly improved, and a truckload of bugs has been fixed.

### Features
- framework for attaching metadata to D2R Server-genearted RDF and HTML documents (Olaf Hartig and Hannes Mühleisen)
- D2R Server provides direct HTTP access to BLOB/CLOB contents via d2rq:DownloadMap
- support for Firebird/Interbase database
- generate-mapping can output RDFS representation of the DB using -v switch (David Venable)
- initial SPARQL 1.1 support via ARQ, not yet optimized

### Enhancements
- generate-mapping can be limited to a single schema using -s argument
- support PostgreSQL UUID type (Kurt Jacobson)
- ignore sysdiagrams table on SQL Server (Giovanni Mels)
- better command line help for generate-mapping
- upgrade core libraries: ARQ 2.9, Jena 2.7, Joseki 3.4.4, Jetty 8.1.1
- upgrade other libraries: Velocity 1.7, SLF4J 1.6.4, log4j 1.2.16, HSQLDB 2.8.8
- replace all references to Notation 3 (.n3) with Turtle (.ttl)
- better handling of Oracle tables with very long names (Jacobus Geluk)
- better handling of many Oracle datatypes (Jacobus Geluk)
- Snorql now works on IE (Jacobus Geluk)
- Clickable links in Snorql XSLT output (Jacobus Geluk)
- show some progress indication on generate-mapping (Jacobus Geluk)
- restructured and much improved documentation
- added support for TIME columns (as xsd:time) and BINARY (as xsd:hexBinary)
- support NaN and INF for DOUBLE columns on HSQLDB
- added d2rq:intervalColumn, d2rq:bitColumn, d2rq:booleanColumn
- d2rq:startupSQLScript and -l switch allow specifying a SQL script to execute before startup
- treat MySQL TINYINT(1) type as xsd:boolean
- add support for BIT datatype on (hopefully) all DBs that support it

### Bugfixes
- better handling of config file location under Windows
- NPE for Oracle DATE and TIMESTAMP when database contains NULL (Jan-Gregor Fischer)
- fix issue where Tomcat wouldn't find some of the jars
- translation table implementations wouldn't work if they inherit from a class that implements the interface (zazi)
- wrong queries with --fast if property bridge requires joining the same table twice (Giovanni Mels)
- NPE if tableIndexStatistics are present (Giovanni Mels)
- UTF-8 encoding issue in Snorql (Giovanni Mels)
- d2rq:alias on the target of d2rq:refersToClassMap didn't work properly
- fix issue where Tomcat would complain about missing commons-logging
- properly shut down webapp if keep-alive thread is used (Giovanni Mels)
- make d2rq:constantValue work properly on class maps (Giovanni Mels)
- fixed a bug in DistributiveLawApplyer when using functions (bound(), isIRI()...) (Giovanni Mels)
- translate all clauses of an (A || B) filter correctly (Giovanni Mels)
- stop D2R Server forgetting --fast setting on mapping file reload
- cleaned up the D2RQ namespace RDFS file to reflect the implementation
- Oracle datatype fixes: DATE, TIMESTAMP, BLOB, BINARY_FILE, BINARY_DOUBLE
- fix several exceptions when querying for typed literals outside of the SQL type's range
- fix querying for large integer literals
- added extensive datatype test suites for HSQLDB and MySQL
- fix for DATE handling on SQL Server
- fix MySQL issue with dates >=24h and dates <0h
- fix MySQL issue with all-zero DATEs, DATETIMEs, TIMESTAMPs and YEARs

### Performance
- translate filters on literals to SQL (Giovanni Mels)
- support for SPARQL built-in functions like datatype(), lang(), isIRI() (Giovanni Mels)
- combine relations more aggressively when d2rq:containsDuplicates is true

### Other
- move main website to http://d2rq.org/
- D2R Server is no longer a stand-alone download, but bundled directly with D2RQ
- moved source code and issue tracking to GitHub, http://github.com/d2rq/d2rq
- now officially requires Java 1.5
- removed outdated Sesame support
- removed RDQL support as ARQ no longer supports it
- removed dedicated ODBC-JDBC support (still available via 3rd-party ODBC-JDBC bridges)


## D2RQ v0.7 - 2009-08-12

Version 0.7 provides several bugfixes, better dump performance, several new features as well as new optimizations that must be enabled using d2rq:useAllOptimizations, or via D2R Server's --fast switch.

### Features
- Added support for Microsoft SQL Server
- Added support for dynamic properties (by Jörg Henß)
- Added support for d2rq:limit, d2rq:limitInverse, d2rq:orderAsc and d2rq:orderDesc (thanks to Matthias Quasthoff)
- Added d2rq:useAllOptimizations option for bleeding-edge optimizations
- Added d2rq:fetchSize to control the number of rows retrieved per database query. Activated by default from dump_rdf.

### Enhancements
- Added SQL cursor support and MySQL streaming to facilitate dumping of large databases (based on a patch by Alistair Miles)
- Added support for !!a => a in DeMorganLawApplyer; improved DistributiveLawApplyer
- Added output warning about uriPatterns without columns during both mapping generation and loading
- Updated to MySQL Connector/J 5.1.7 due to reported hanging errors
- Updated to ARQ 2.7 (Jena 2.6), Joseki CVS (2008-12), SDB 1.3
- Removed dependencies on Oracle and MySQL libraries
- Removed non-open source JDBC drivers from distribution
- Added note to documentation: Vocabulary serving does not work with SPARQL 

### Bugfixes
- Added Database-specific LIMIT implementations
- Fixed filtering on variables with an sqlExpression property bridge (#2620006, Herwig Leimer)
- Modified treatment of nonmoveable filter expressions in D2RQTreeOptimizer, skip variable retention on subUsage in VarFinder (Herwig Leimer)
- Fully removed buggy leftjoin optimization (#2634088, #2645486)
- Let the JDBC driver convert boolean values separately as their representation differs greatly amongst DBs (thanks to Alistair Miles)
- Fixed JDBC-ODBC Bridge interoperability (MS Access etc.) - refactored ResultRowMap to return values with a single get???() call
- Relaxed table naming conditions to support hyphens and numbers in table names (thanks to Heinz Doerrer, bug #2664865)
- Properly iterate over trivial relations (fixes ResultRow to Tripe casting bug)
- Don't create a link table if it exports foreign keys (#2787278) 
- Support case-insensitive RelationName comparison for MySQL and allow map generation from keys with unspecified capitalization
- Fixed a bug with the keepAliveAgent which didn't terminate when dump_rdf is used
- Handle multiple projection specs for SPARQL variable within TransformExprToSQLApplyer 
- Moved DISTINCT before TOP in SQL queries
- Fixed a bug in DistributiveLawApplyer

### Performance
- Added self-join optimization (#2798308). Enable using --fast command-line option in D2R Server, or d2rq:useAllOptimizations.
- Added support for directed arrows in d2rq:join to indicate foreign key relationship and to utilize them for correct join optimization (#1794042)
- Omit empty NodeRelations when translating graph patterns
- Keep uniqueness when working with just one Relation in TripleRelationJoiner (requires --fast) 
- Only check for predicate URI matches only if dynamic properties are present in the mapping 
- Translate filter expressions involving constant nodes to SQL


## D2RQ v0.6 - 2009-02-19

### Features
- d2rq:sqlExpression
- d2rq:uriSqlExpression for properties with URI values, similar to d2rq:sqlExpression but generates URIs instead of literals (Andreas Langegger)
- Support for serving vocabulary classes and properties; including definition labels, comments and additional properties

### Enhancements
- New ARQ-based QueryEngine, dropped the old FastPath engine
- Mapping generator now supports multi-column foreign key constraints
- Specify JDBC connection properties through the jdbc: namespace
- Better error reporting
- Oracle support - automatic registration of the JDBC driver, DATE, TIMESTAMP and CLOB field types
- Support for PostgreSQL's TIMESTAMP WITH TIME ZONE type
- Proper treatment of xsd:boolean datatype
- Show XML declaration and make sure that utf-8 encoding is used for XML
- Map parser now complains about misspelled D2RQ vocabulary terms
- Remove MySQL hack for zero date values from the mapping generator, use zeroDateTimeBehavior property instead
- Change dump-rdf default format to N-TRIPLE (faster, better streaming)

### Performance
- Push-down and transformation of SPARQL filter expressions into SQL, if possible
- Transformation of SPARQL optionals into SQL left joins; some cases only

### Bugfixes
- many bugfixes

### Other
- Replaced custom Logger with Log4j one
- Give more memory to VM in dump-rdf and generate-mapping scripts

## D2RQ v0.5.1 - 2007-10-24

### Enhancements
- added d2rq:resultSizeLimit option
- smarter mapping generator

### Bugfixes
- columns with non-URI characters can now be used in URI patterns
- many bugfixes


## D2RQ v0.5 - 2006-10-26

### Features
- supports the SPARQL query language.
- added dump-rdf command line script.
- added automatic mapping generator.
- added Jena Assembler for D2RQ models.

### Enhancements
- improved compatibility with MySQL, Oracle and PostgreSQL.
- compatible with Jena 2.4 and ARQ 1.4
- many other improvements.

### Bugfixes
- many bugfixes.


## D2RQ v0.4 - 2005-10-25

### Features
- Wrapper for Sesame added.


## D2RQ v0.3 - 2005-04-29

### Features
- d2rq:queryHandler added.
- d2rq:expressionTranslator added.
- d2rq:alias added.

### Performance
- New experimental D2RQQueryHandler for speeding up complex RDQL queries.


## D2RQ v0.2 - 2004-08-12

### Features
- d2rq:TranslationTable between DB and RDF values added.
- d2rq:condition added; allows filtering by SQL expression.
- d2rq:AdditionalProperty added.
- d2rq:containsDuplicates hint added.
- d2rq:class added; alternative to d2rq:classMap.
- d2rq:property added; alternative to d2rq:propertyBridge.

### Performance
- d2rq:valueContains performance optimization hint added.
- d2rq:valueMaxLength performance optimization hint added.
- d2rq:valueRegex performance optimization hint added.
- major speed improvement for queries with large result sets.
- major speed improvement for maps with URL columns.

### Other
- logging facility added.
- security fix: SQL injection vulnerability fixed.
- bug fix: issue with missing statements when using joins and NULL values.
- improved map consistency checking and better error messages
- GraphD2RQ and ModelD2RQ can also be initialised from RDF/XML map file.
- GraphD2RQ and ModelD2RQ can also be initialised from Jena map model.
- architectural changes for better maintainability.
- JUnit test suite added.
- many small fixes, improvements, speedups and refactorings.


## D2RQ v0.1 - 2004-06-22

- Initial D2RQ Release.
