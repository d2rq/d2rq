Remarks to BBN changes
======================================
General
 - The changes do not refer to the most recent version of D2RQ
 - too many changes. quality checking & merging needs more time and people
 - Read the BBN changelog
 - Check the shorter BBN generated SQL-Statements
   Which D2RQ-Map?

Code
 - TripleRelationship introduced

-------------
GraphD2RQ
 - List of TripleRelationship as Parameter to QueryCombiner (caching?)
 - bridgeDoesFit() (Changelog 1)
   uses TripleRelationship

CombinedTripleResultSet (take cvs version)
  - compiled is never used but introduced with accessor methods. 

QueryCombiner
  - uses relationships (see GraphD2RQ)
  
SQLResultSet
 - this.resultSet.getStatement().close();

SQLStatementMaker
 - multiple Statements. How is this produced?

TripleQuery
 - Set (columns) -> List. why?
   NodeMaker.getColumns() signature changes
   ok? see Column equals

Alias
 - Oracle note

BlankNodeIdentifier
 - column (Set/List)

BlankNodeMaker
 - couldFit use of isVariable() correct?
 + prefixTables

Column
  never had an equals() method.
  So Set of Column and List of Column only differ if two identical Object references are added.

... other Classes more changes

rdql
====
TablePrefixer
 - changed much

utils/StringUtils
 - new
