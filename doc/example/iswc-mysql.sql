-- phpMyAdmin SQL Dump
-- version 2.9.1.1
-- http://www.phpmyadmin.net
-- 
-- Host: localhost
-- Generation Time: Feb 21, 2007 at 07:04 PM
-- Server version: 5.0.27
-- PHP Version: 5.2.0
-- 
-- Database: `iswc`
-- 

-- --------------------------------------------------------

-- 
-- Table structure for table `conferences`
-- 

CREATE TABLE `conferences` (
  `ConfID` int(11) NOT NULL default '0',
  `Name` varchar(100) default NULL,
  `URI` varchar(200) default NULL,
  `Date` varchar(50) default NULL,
  `Location` varchar(50) default NULL,
  `Datum` datetime default NULL,
  PRIMARY KEY  (`ConfID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `conferences`
-- 

INSERT INTO `conferences` (`ConfID`, `Name`, `URI`, `Date`, `Location`, `Datum`) VALUES 
(23541, 'International Semantic Web Conference 2002', 'http://annotation.semanticweb.org/iswc/iswc.daml#ISWC_2002', 'June 9-12, 2002', 'Sardinia', '2002-10-09 00:00:00'),
(23542, '15th International World Wide Web Conference', NULL, 'May 23-26, 2006', 'Edinburgh', '2006-05-23 00:00:00');

-- --------------------------------------------------------

-- 
-- Table structure for table `organizations`
-- 

CREATE TABLE `organizations` (
  `OrgID` int(11) NOT NULL default '0',
  `Type` varchar(50) default NULL,
  `Name` varchar(200) default NULL,
  `Address` mediumtext,
  `Location` varchar(50) default NULL,
  `Postcode` varchar(10) default NULL,
  `Country` varchar(50) default NULL,
  `URI` varchar(100) default NULL,
  `Belongsto` int(11) default NULL,
  `Homepage` varchar(200) default NULL,
  PRIMARY KEY  (`OrgID`),
  KEY `Belongsto` (`Belongsto`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `organizations`
-- 

INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Postcode`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES 
(1, NULL, 'USC Information Sciences Institute', '4676 Admirality Way', 'Marina Del Rey', NULL, 'United States', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#ISI', NULL, NULL),
(2, 'U', 'University of Karlsruhe', NULL, NULL, NULL, NULL, 'http://annotation.semanticweb.org/iswc/iswc.daml#University_of_Karlsruhe', NULL, NULL),
(3, 'U', 'International University in Germany', NULL, NULL, NULL, NULL, 'http://www.i-u.de/schools/eberhart/iswc2002/#International University in Germany', NULL, NULL),
(4, 'I', 'AIFB', NULL, 'Karlsruhe', NULL, 'Germany', 'http://annotation.semanticweb.org/iswc/iswc.daml#AIFB', NULL, 'http://www.aifb.uni-karlsruhe.de/'),
(5, 'D', 'Department of Computer Science', 'De Boelelaan 1081a', 'Amsterdam', '1081 HV', 'The Netherlands', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#CSVUNL', 6, NULL),
(6, 'U', 'Vrije Universiteit Amsterdam', 'De Boelelaan 1105', 'Amsterdam', '1081 HV', 'The Netherlands', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#VrijeUniversiteitAmsterdam', NULL, NULL),
(7, NULL, 'Hewlett-Packard Laboratories, Bristol', 'Filton Road', 'Bristol', 'BS34 8QZ', 'UK', 'http://www.hpl.hp.co.uk#HPL', NULL, 'http://www.hpl.hp.com/'),
(8, 'I', 'Institute for the Protection and Security of the Citizen', 'Via Enrico Fermi, 1\n21020 - Ispra (Italy)', NULL, NULL, NULL, 'http://dma.jrc.it#Institute for the Protection and Security of the Citizen', NULL, NULL),
(9, 'D', 'Dipartimento di Ingegneria dell''Informazione', 'Via Vignolese 905', 'Modena', NULL, 'Italy', 'http://www.dbgroup.unimo.it/iswc/iswc.html#DII', NULL, NULL);

-- --------------------------------------------------------

-- 
-- Table structure for table `papers`
-- 

CREATE TABLE `papers` (
  `PaperID` int(11) NOT NULL default '0',
  `Title` varchar(200) default NULL,
  `Abstract` mediumtext,
  `URI` varchar(200) default NULL,
  `Year` int(11) default NULL,
  `Conference` int(11) default NULL,
  `Publish` tinyint(1) default NULL,
  PRIMARY KEY  (`PaperID`),
  KEY `Conference` (`Conference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `papers`
-- 

INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES 
(1, 'Trusting Information Sources One Citizen at a Time', 'This paper describes an approach to derive assessments about information \n      sources based on individual feedback about the sources. We describe \n      TRELLIS, a system that helps users annotate their analysis of \n      alternative information sources that can be contradictory and \n      incomplete. As the user makes a decision on which sources to dismiss and \n      which to believe in making a final decision, TRELLIS captures the \n      derivation of the decision in a semantic markup. TRELLIS then uses these \n      annotations to derive an assessment of the source based on the \n      annotations of many individuals. Our work builds on the Semantic Web and \n      presents a tool that helps users create annotations that are in a mix of \n      formal and human language, and exploits the formal representations to \n      derive measures of trust in the content of Web resources and their \n      original source.', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Trusting Information Sources One Citizen at a Time', 2002, 23541, 1),
(2, 'Automatic Generation of Java/SQL based Inference Engines from RDF Schema and RuleML', 'This paper describes two approaches for automatically converting RDF Schema \nand RuleML sources into an inference engine and storage repository. Rather than \nusing traditional inference systems, our solution bases on mainstream \ntechnologies like Java and relational database systems. While this necessarily \nimposes some restrictions, the ease of integration into an existing IT landscape \nis a major advantage. We present the conversion tools and their limitations. \nFurthermore, an extension to RuleML is proposed, that allows Java-enabled \nreaction rules, where calls to Java libraries can be performed upon a rule \nfiring. This requires hosts to be Java-enabled when rules and code are moved \nacross the web. However, the solution allows for great engineering \nflexibility.', 'http://www.i-u.de/schools/eberhart/iswc2002/#Automatic Generation of Java/SQL based Inference Engines from RDF Schema and RuleML', 2002, 23541, 1),
(4, 'Three Implementations of SquishQL, a Simple RDF Query Language', 'RDF provides a basic way to represent data for the Semantic Web. We have \n      been experimenting with the query paradigm for working with RDF data in \n      semantic web applications. Query of RDF data provides a declarative \n      access mechanism that is suitable for application usage and remote \n      access. We describe work on a conceptual model for querying RDF data \n      that refines ideas first presented in at the W3C workshop on Query \n      Languages and the design of one possible syntax, derived from rdfDB, \n      that is suitable for application programmers. Further, we present \n      experience gained in three implementations of the query language.', 'http://www-uk.hpl.hp.com/people/afs/Abstracts/ISWC2002-SquishQL-Abstract.html#SquishQL', 2003, 23541, 1),
(5, 'A Data Integration Framework for E-commerce Product Classification', 'A marketplace is \n      the place in which the demand and supply of buyers and vendors \n      participating in a business process may meet. Therefore, electronic \n      marketplaces are virtual communities in which buyers may meet proposals \n      of several suppliers and make the best choice. In the electronic \n      commerce world, the comparison between different products is blocked due \n      to the lack of standards (on the contrary, the proliferation of \n      standards) describing and classifying them. Therefore, the need for B2B \n      and B2C marketplaces is to reclassify products and goods according to \n      different standardization models. This paper aims to face this problem \n      by suggesting the use of a semi-automatic methodology, supported by a \n      tool (SI-Designer), to define the mapping among different e-commerce \n      product classification standards. This methodology was developed for the \n      MOMIS-system within the Intelligent Integration of Information research \n      area. We describe our extension to the methodology that makes it \n      applyable in general to product classification standard, by selecting a \n      fragment of ECCMA/UNSPSC and ecl@ss standard.', 'http://www.dbgroup.unimo.it/iswc/iswc.html#A Data Integration Framework for E-commerce Product Classification', 2002, 23541, 1),
(6, 'Integrating Vocabularies: Discovering and Representing Vocabulary Maps', 'The Semantic Web would enable new ways of doing business on the<br>Web \n      that require development of advanced business document<br>integration \n      technologies performing intelligent document<br>transformation. The \n      documents use different vocabularies that<br>consist of large \n      hierarchies of terms. Accordingly, vocabulary<br>mapping and \n      transformation becomes an important task in the whole<br>business \n      document transformation process. It includes several<br>subtasks: map \n      discovery, map representation, and map execution<br>that must be \n      seamlessly integrated into the document integration<br>process. In this \n      paper we discuss the process of discovering the<br>maps between two \n      vocabularies assuming availability of two sets of<br>documents, each \n      using one of the vocabularies. We take the<br>vocabularies of product \n      classification codes as a playground and<br>propose a reusable map \n      discovery technique based on Bayesian text<br>classification approach. \n      We show how the discovered maps can be<br>integrated into the document \n      transformation process.', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#OmelayenkoISWC2002', 2003, 23541, 0),
(7, 'The Semantic Web Revisited', 'The original Scientific American article on the Semantic Web appeared in 2001. It described the evolution of a Web that consisted largely of documents for humans to read to one that included data and information for computers to manipulate. The Semantic Web is a Web of actionable information--information derived from data through a semantic theory for interpreting the symbols.This simple idea, however, remains largely unrealized. Shopbots and auction bots abound on the Web, but these are essentially handcrafted for particular tasks; they have little ability to interact with heterogeneous data and information types. Because we haven''t yet delivered large-scale, agent-based mediation, some commentators argue that the Semantic Web has failed to deliver. We argue that agents can only flourish when standards are well established and that the Web standards for expressing shared meaning have progressed steadily over the past five years. Furthermore, we see the use of ontologies in the e-science community presaging ultimate success for the Semantic Web--just as the use of HTTP within the CERN particle physics community led to the revolutionary success of the original Web. This article is part of a special issue on the Future of AI.', 'http://eprints.ecs.soton.ac.uk/12614/01/Semantic_Web_Revisted.pdf', 2006, NULL, 1),
(8, 'D2R Server - Publishing Relational Databases on the Web as SPARQL Endpoints', 'The Resource Description Framework and the SPARQL query language provide a standardized way for exposing and linking data sources on the Web. D2R Server is a turn-key solution for making the content of existing, non-RDF databases accessible as SPARQL endpoints. The server takes SPARQL queries from the Web and rewrites them via a mapping into SQL queries against a relational database. This on-the-fly translation allows RDF applications to access the content of large databases without having to replicate them into RDF. D2R Server can be used to integrate existing databases into RDF systems, and to add SPARQL interfaces to database-backed software products. In the talk, we will give an introduction into the D2RQ mapping language, which is used to define mappings between relational and RDF schemata, and demonstrate how D2R Server can be used to extend a WordPress blog with a SPARQL interface.', 'http://www.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/resources/d2r-server-slides-www2006.pdf', 2006, 23542, 1);

-- --------------------------------------------------------

-- 
-- Table structure for table `persons`
-- 

CREATE TABLE `persons` (
  `PerID` int(11) NOT NULL default '0',
  `Type` varchar(50) default NULL,
  `FirstName` varchar(100) default NULL,
  `LastName` varchar(100) default NULL,
  `Address` varchar(200) default NULL,
  `Email` varchar(100) default NULL,
  `Homepage` varchar(50) default NULL,
  `Phone` varchar(200) default NULL,
  `URI` varchar(200) default NULL,
  `Photo` varchar(200) default NULL,
  PRIMARY KEY  (`PerID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `persons`
-- 

INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES 
(1, 'Full_Professor', 'Yolanda', 'Gil', NULL, 'gil@isi.edu', 'http://www.isi.edu/~gil', '310-448-8794', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Yolanda Gil', 'http://www.isi.edu/~gil/y.g.v4.tiff'),
(2, NULL, 'Varun', 'Ratnakar', NULL, 'varunr@isi.edu', 'http://www.isi.edu/~varunr', NULL, 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Varun Ratnakar', NULL),
(3, 'Researcher', 'Jim', 'Blythe', NULL, 'blythe@isi.edu', 'http://www.isi.edu/~varunr', NULL, 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Jim Blythe', NULL),
(4, 'Researcher', 'Andreas', 'Eberhart', 'International University in Germany Campus 2 76646 Bruchsal Germany', 'eberhart@i-u.de', 'http://www.i-u.de/schools/eberhart/', '+49 7251 700 222', 'http://www.i-u.de/schools/eberhart/iswc2002/#Andreas Eberhart', NULL),
(5, 'Researcher', 'Borys', 'Omelayenko', 'Vrije Universiteit, Division of Mathematics and Computer Science, De Boelelaan 1081a,1081hv, Amsterdam, The Netherlands', 'borys@cs.vu.nl', 'http://www.cs.vu.nl/~borys', NULL, 'http://www.cs.vu.nl/~borys#Bomelayenko', NULL),
(6, 'Researcher', 'Andy', 'Seaborne', 'Hewlett-Packard Laboratories, Bristol, BS34 8QZ, UK', 'andy.seaborne@hpl.hp.com', 'http://www-uk.hpl.hp.com/people/afs/', NULL, 'http://www-uk.hpl.hp.com/people#andy_seaborne', 'http://semtech2011.semanticweb.com/uploads/images/bios/3205.jpg'),
(9, NULL, 'Alberto', 'Reggiori', NULL, 'areggiori@webweaving.org', 'http://reggiori.webweaving.org/', NULL, 'http://reggiori.webweaving.org#Alberto Reggiori', NULL),
(10, 'Full_Professor', 'Sonia', 'Bergamaschi', 'DII- Universita di Modena e Reggio Emilia via Vignolese 905 41100 Modena', 'bergamaschi.sonia@unimo.it', 'http://www.dbgroup.unimo.it/Bergamaschi.html', '+39 059 2056132', 'http://www.dbgroup.unimo.it/iswc/iswc.html#S. Bergamaschi', NULL),
(11, NULL, 'Francesco', 'Guerra', 'DII- Universita di Modena e Reggio Emilia via Vignolese 905 41100 Modena Italy', 'guerra.francesco@unimo.it', 'http://www.dbgroup.unimo.it/~guerra/', '+39 059 20561543', 'http://www.dbgroup.unimo.it/iswc/iswc.html#F. Guerra', NULL),
(12, 'Researcher', 'Christian', 'Bizer', 'Freie Universität Berlin', 'chris@bizer.de', 'http://www.bizer.de/', NULL, 'http://www.bizer.de/foaf.rdf#chris', 'http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/pics/bizer_christian_200x300.png');

-- --------------------------------------------------------

-- 
-- Table structure for table `rel_paper_topic`
-- 

CREATE TABLE `rel_paper_topic` (
  `PaperID` int(11) NOT NULL default '0',
  `TopicID` int(11) NOT NULL default '0',
  `RelationType` int(11) default NULL,
  PRIMARY KEY  (`PaperID`,`TopicID`),
  KEY `TopicID` (`TopicID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `rel_paper_topic`
-- 

INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES 
(1, 5, 1),
(1, 15, 2),
(2, 3, 3),
(2, 5, 2),
(4, 10, 2),
(4, 11, 1),
(4, 14, 2),
(5, 2, 3),
(5, 3, 1),
(5, 13, 2),
(6, 5, 2),
(6, 11, 1),
(6, 13, 3);

-- --------------------------------------------------------

-- 
-- Table structure for table `rel_person_organization`
-- 

CREATE TABLE `rel_person_organization` (
  `PersonID` int(11) NOT NULL default '0',
  `OrganizationID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`OrganizationID`),
  KEY `OrganizationID` (`OrganizationID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `rel_person_organization`
-- 

INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES 
(1, 1),
(2, 1),
(3, 1),
(4, 3),
(5, 5),
(5, 6),
(6, 7),
(9, 8),
(10, 9),
(11, 9);

-- --------------------------------------------------------

-- 
-- Table structure for table `rel_person_paper`
-- 

CREATE TABLE `rel_person_paper` (
  `PersonID` int(11) NOT NULL default '0',
  `PaperID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`PaperID`),
  KEY `PaperID` (`PaperID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `rel_person_paper`
-- 

INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES 
(1, 1),
(2, 1),
(4, 2),
(6, 4),
(9, 4),
(10, 5),
(11, 5),
(5, 6),
(12, 8);

-- --------------------------------------------------------

-- 
-- Table structure for table `rel_person_topic`
-- 

CREATE TABLE `rel_person_topic` (
  `PersonID` int(11) NOT NULL default '0',
  `TopicID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`TopicID`),
  KEY `TopicID` (`TopicID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `rel_person_topic`
-- 

INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES 
(1, 1),
(3, 1),
(4, 1),
(1, 2),
(3, 2),
(1, 3),
(3, 3),
(2, 5),
(5, 5),
(6, 5),
(9, 5),
(10, 5),
(11, 5),
(2, 6),
(2, 7),
(5, 7),
(2, 8),
(10, 10),
(11, 10),
(5, 13),
(10, 15);

-- --------------------------------------------------------

-- 
-- Table structure for table `topics`
-- 

CREATE TABLE `topics` (
  `TopicID` int(11) NOT NULL default '0',
  `TopicName` varchar(50) default NULL,
  `URI` varchar(200) default NULL,
  `ParentID` int(11) default NULL,
  PRIMARY KEY  (`TopicID`),
  KEY `ParentID` (`ParentID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- 
-- Dumping data for table `topics`
-- 

INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`, `ParentID`) VALUES 
(1, 'Knowledge Representation Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Representation_Languages', 3),
(2, 'Knowledge Systems', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Systems', 15),
(3, 'Artificial Intelligence', 'http://annotation.semanticweb.org/iswc/iswc.daml#Artificial_Intelligence', NULL),
(4, 'Semantic Annotation', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Annotation', 5),
(5, 'Semantic Web', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web', 8),
(6, 'Semantic Web Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web_Languages', 5),
(7, 'Web Services', 'http://annotation.semanticweb.org/iswc/iswc.daml#Web_Services', 8),
(8, 'World Wide Web', 'http://annotation.semanticweb.org/iswc/iswc.daml#World_Wide_Web', NULL),
(9, 'Text Mining', 'http://annotation.semanticweb.org/iswc/iswc.daml#Text_Mining', 16),
(10, 'Databases', 'http://annotation.semanticweb.org/iswc/iswc.daml#Databases', NULL),
(11, 'Semantic Web Infrastructure', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web_Iinfrastructure', 5),
(13, 'E-Business', 'http://annotation.semanticweb.org/iswc/iswc.daml#e-Business', NULL),
(14, 'Query Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Query_Languages', 16),
(15, 'Knowledge Management', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Management', NULL),
(16, 'Knowledge Discovery', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Discovery', 3);

-- 
-- Constraints for dumped tables
-- 

-- 
-- Constraints for table `organizations`
-- 
ALTER TABLE `organizations`
  ADD CONSTRAINT `organizations_ibfk_1` FOREIGN KEY (`Belongsto`) REFERENCES `organizations` (`OrgID`);

-- 
-- Constraints for table `papers`
-- 
ALTER TABLE `papers`
  ADD CONSTRAINT `papers_ibfk_1` FOREIGN KEY (`Conference`) REFERENCES `conferences` (`ConfID`);

-- 
-- Constraints for table `rel_paper_topic`
-- 
ALTER TABLE `rel_paper_topic`
  ADD CONSTRAINT `rel_paper_topic_ibfk_1` FOREIGN KEY (`PaperID`) REFERENCES `papers` (`PaperID`),
  ADD CONSTRAINT `rel_paper_topic_ibfk_2` FOREIGN KEY (`TopicID`) REFERENCES `topics` (`TopicID`);

-- 
-- Constraints for table `rel_person_organization`
-- 
ALTER TABLE `rel_person_organization`
  ADD CONSTRAINT `rel_person_organization_ibfk_1` FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`),
  ADD CONSTRAINT `rel_person_organization_ibfk_2` FOREIGN KEY (`OrganizationID`) REFERENCES `organizations` (`OrgID`);

-- 
-- Constraints for table `rel_person_paper`
-- 
ALTER TABLE `rel_person_paper`
  ADD CONSTRAINT `rel_person_paper_ibfk_1` FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`),
  ADD CONSTRAINT `rel_person_paper_ibfk_2` FOREIGN KEY (`PaperID`) REFERENCES `papers` (`PaperID`);

-- 
-- Constraints for table `rel_person_topic`
-- 
ALTER TABLE `rel_person_topic`
  ADD CONSTRAINT `rel_person_topic_ibfk_1` FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`),
  ADD CONSTRAINT `rel_person_topic_ibfk_2` FOREIGN KEY (`TopicID`) REFERENCES `topics` (`TopicID`);

-- 
-- Constraints for table `topics`
-- 
ALTER TABLE `topics`
  ADD CONSTRAINT `topics_ibfk_1` FOREIGN KEY (`ParentID`) REFERENCES `topics` (`TopicID`);
