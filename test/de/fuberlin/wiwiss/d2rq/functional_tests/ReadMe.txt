----------------------------------------
D2RQ Functional Tests Read Me
----------------------------------------

The functional tests are using the ISWC database from the doc/manual directory.

In order to run the test, you have to

1. set up a MySQL or ODBC database using the 
   + iswc.sql.zip.sql MySQL database dump or the
   + ISWC.mdb Access database 
   from the doc/manual directory.

2. Change the datsource configuration in the mapping file 
   doc/manual/ISWC-d2rq.n3 to connect to your database.

----------------------------------------
Chris, 08-12-2004