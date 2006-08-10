# phpMyAdmin SQL Dump
# version 2.5.5-pl1
# http://www.phpmyadmin.net
#
# Host: localhost
# Generation Time: Aug 04, 2004 at 12:08 PM
# Server version: 4.0.14
# PHP Version: 4.3.4
# 
# Database : `iswc`
# 

# --------------------------------------------------------

#
# Table structure for table `conferences`
#

CREATE TABLE `conferences` (
  `ConfID` int(11) NOT NULL default '0',
  `Name` varchar(100) default NULL,
  `URI` varchar(200) default NULL,
  `Date` varchar(50) default NULL,
  `Location` varchar(50) default NULL,
  `Datum` datetime default NULL,
  PRIMARY KEY  (`ConfID`)
) TYPE=InnoDB;

#
# Dumping data for table `conferences`
#

INSERT INTO `conferences` (`ConfID`, `Name`, `URI`, `Date`, `Location`, `Datum`) VALUES (23541, 'International Semantic Web Conference 2002', 'http://annotation.semanticweb.org/iswc/iswc.daml#ISWC_2002', 'June 9-12, 2002', 'Sardinia', '2002-10-09 00:00:00');

# --------------------------------------------------------

#
# Table structure for table `organizations`
#

CREATE TABLE `organizations` (
  `OrgID` int(11) NOT NULL default '0',
  `Type` varchar(50) default NULL,
  `Name` varchar(200) default NULL,
  `Address` mediumtext,
  `Location` varchar(50) default NULL,
  `Country` varchar(50) default NULL,
  `URI` varchar(100) default NULL,
  `Belongsto` int(11) default NULL,
  `Homepage` varchar(200) default NULL,
  PRIMARY KEY  (`OrgID`)
) TYPE=InnoDB;

#
# Dumping data for table `organizations`
#

INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (1, 'Organization', 'USC Information Sciences Institute', '4676 Admirality Way, Marina Del Rey', 'California', 'United States', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#ISI', NULL, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (2, 'University', 'University of Karlsruhe', NULL, NULL, NULL, 'http://annotation.semanticweb.org/iswc/iswc.daml#University_of_Karlsruhe', NULL, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (3, 'University', 'International University in Germany', NULL, NULL, NULL, 'http://www.i-u.de/schools/eberhart/iswc2002/#International University in Germany', NULL, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (4, 'Institut', 'AIFB', NULL, 'Karlsruhe', 'Germany', 'http://annotation.semanticweb.org/iswc/iswc.daml#AIFB', 3, 'http://www.aifb.uni-karlsruhe.de/');
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (5, 'Department', 'Department of Computer Science', 'De Boelelaan 1081a', NULL, 'The Netherlands', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#CSVUNL', 6, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (6, 'University', 'Universiteit Amsterdam', 'De Boelelaan 1105', NULL, 'The Netherlands', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#VrijeUniversiteitAmsterdam', NULL, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (7, 'Organization', 'Hewlett-Packard Laboratories, Bristol', NULL, 'Bristol', 'UK', 'http://www.hpl.hp.co.uk#HPL', NULL, 'http://www.hpl.hp.com/');
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (8, 'Institut', 'Institute for the Protection and Security of the Citizen', 'Institute for the Protection and Security of the Citizen\nVia Enrico Fermi, 1\n21020 - Ispra (Italy)', NULL, NULL, 'http://dma.jrc.it#Institute for the Protection and Security of the Citizen', NULL, NULL);
INSERT INTO `organizations` (`OrgID`, `Type`, `Name`, `Address`, `Location`, `Country`, `URI`, `Belongsto`, `Homepage`) VALUES (9, 'Department', 'Dipartimento di Ingegneria dell\'Informazione', ' Via Vignolese 905 – Modena Italy', NULL, 'Italy', 'http://www.dbgroup.unimo.it/iswc/iswc.html#DII', NULL, NULL);

# --------------------------------------------------------

#
# Table structure for table `papers`
#

CREATE TABLE `papers` (
  `PaperID` int(11) NOT NULL default '0',
  `Title` varchar(200) default NULL,
  `Abstract` mediumtext,
  `URI` varchar(200) default NULL,
  `Year` int(11) default NULL,
  `Conference` int(11) default NULL,
  `Publish` tinyint(1) default NULL,
  PRIMARY KEY  (`PaperID`)
) TYPE=InnoDB;

#
# Dumping data for table `papers`
#

INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES (1, 'Trusting Information Sources One Citizen at a Time', 'This paper describes an approach to derive assessments about information \n      sources based on individual feedback about the sources. We describe \n      TRELLIS, a system that helps users annotate their analysis of \n      alternative information sources that can be contradictory and \n      incomplete. As the user makes a decision on which sources to dismiss and \n      which to believe in making a final decision, TRELLIS captures the \n      derivation of the decision in a semantic markup. TRELLIS then uses these \n      annotations to derive an assessment of the source based on the \n      annotations of many individuals. Our work builds on the Semantic Web and \n      presents a tool that helps users create annotations that are in a mix of \n      formal and human language, and exploits the formal representations to \n      derive measures of trust in the content of Web resources and their \n      original source.', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Trusting Information Sources One Citizen at a Time', 2002, 23541, 1);
INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES (2, 'Automatic Generation of Java/SQL based Inference Engines from RDF Schema and RuleML', 'This paper describes two approaches for automatically converting RDF Schema \nand RuleML sources into an inference engine and storage repository. Rather than \nusing traditional inference systems, our solution bases on mainstream \ntechnologies like Java and relational database systems. While this necessarily \nimposes some restrictions, the ease of integration into an existing IT landscape \nis a major advantage. We present the conversion tools and their limitations. \nFurthermore, an extension to RuleML is proposed, that allows Java-enabled \nreaction rules, where calls to Java libraries can be performed upon a rule \nfiring. This requires hosts to be Java-enabled when rules and code are moved \nacross the web. However, the solution allows for great engineering \nflexibility.', 'http://www.i-u.de/schools/eberhart/iswc2002/#Automatic Generation of Java/SQL based Inference Engines from RDF Schema and RuleML', 2002, 23541, 1);
INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES (4, 'Three Implementations of SquishQL, a Simple RDF Query Language', 'RDF provides a basic way to represent data for the Semantic Web. We have \n      been experimenting with the query paradigm for working with RDF data in \n      semantic web applications. Query of RDF data provides a declarative \n      access mechanism that is suitable for application usage and remote \n      access. We describe work on a conceptual model for querying RDF data \n      that refines ideas first presented in at the W3C workshop on Query \n      Languages and the design of one possible syntax, derived from rdfDB, \n      that is suitable for application programmers. Further, we present \n      experience gained in three implementations of the query language.', 'http://www-uk.hpl.hp.com/people/afs/Abstracts/ISWC2002-SquishQL-Abstract.html#SquishQL', 2003, 23541, 1);
INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES (5, 'A Data Integration Framework for E-commerce Product Classification', 'A marketplace is \n      the place in which the demand and supply of buyers and vendors \n      participating in a business process may meet. Therefore, electronic \n      marketplaces are virtual communities in which buyers may meet proposals \n      of several suppliers and make the best choice. In the electronic \n      commerce world, the comparison between different products is blocked due \n      to the lack of standards (on the contrary, the proliferation of \n      standards) describing and classifying them. Therefore, the need for B2B \n      and B2C marketplaces is to reclassify products and goods according to \n      different standardization models. This paper aims to face this problem \n      by suggesting the use of a semi-automatic methodology, supported by a \n      tool (SI-Designer), to define the mapping among different e-commerce \n      product classification standards. This methodology was developed for the \n      MOMIS-system within the Intelligent Integration of Information research \n      area. We describe our extension to the methodology that makes it \n      applyable in general to product classification standard, by selecting a \n      fragment of ECCMA/UNSPSC and ecl@ss standard.', 'http://www.dbgroup.unimo.it/iswc/iswc.html#A Data Integration Framework for E-commerce Product Classification', 2002, 23541, 1);
INSERT INTO `papers` (`PaperID`, `Title`, `Abstract`, `URI`, `Year`, `Conference`, `Publish`) VALUES (6, 'Integrating Vocabularies: Discovering and Representing Vocabulary Maps', 'The Semantic Web would enable new ways of doing business on the<br>Web \n      that require development of advanced business document<br>integration \n      technologies performing intelligent document<br>transformation. The \n      documents use different vocabularies that<br>consist of large \n      hierarchies of terms. Accordingly, vocabulary<br>mapping and \n      transformation becomes an important task in the whole<br>business \n      document transformation process. It includes several<br>subtasks: map \n      discovery, map representation, and map execution<br>that must be \n      seamlessly integrated into the document integration<br>process. In this \n      paper we discuss the process of discovering the<br>maps between two \n      vocabularies assuming availability of two sets of<br>documents, each \n      using one of the vocabularies. We take the<br>vocabularies of product \n      classification codes as a playground and<br>propose a reusable map \n      discovery technique based on Bayesian text<br>classification approach. \n      We show how the discovered maps can be<br>integrated into the document \n      transformation process.', 'http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#OmelayenkoISWC2002', 2003, 23541, 0);

# --------------------------------------------------------

#
# Table structure for table `persons`
#

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
  `Photo` varchar(50) default NULL,
  PRIMARY KEY  (`PerID`)
) TYPE=InnoDB;

#
# Dumping data for table `persons`
#

INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (1, 'Professor', 'Yolanda', 'Gil', NULL, 'gil@isi.edu', 'http://www.isi.edu/~gil', '310-448-8794', 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Yolanda Gil', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (2, 'Employee', 'Varun', 'Ratnakar', NULL, 'varunr@isi.edu', 'http://www.isi.edu/~varunr', NULL, 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Varun Ratnakar', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (3, 'ResearchAssistent', 'Jim', 'Blythe', NULL, 'blythe@isi.edu', 'http://www.isi.edu/~varunr', NULL, 'http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Jim Blythe', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (4, 'ResearchAssistent', 'Andreas', 'Eberhart', 'International University in Germany Campus 2 76646 Bruchsal Germany', 'eberhart@i-u.de', 'http://www.i-u.de/schools/eberhart/', '+49 7251 700 222', 'http://www.i-u.de/schools/eberhart/iswc2002/#Andreas Eberhart', 'http://www.i-u.de/images/andi.jpg');
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (5, 'ResearchAssistent', 'Borys', 'Omelayenko', 'Vrije Universiteit, Division of Mathematics and Computer Science, De Boelelaan 1081a,1081hv, Amsterdam, The Netherlands', 'borys@cs.vu.nl', 'http://www.cs.vu.nl/~borys', NULL, 'http://www.cs.vu.nl/~borys#Bomelayenko', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (6, 'Researcher', 'Andy', 'Seaborne', 'Hewlett-Packard Laboratories, Bristol, BS34 8QZ, UK', 'andy.seaborne@hpl.hp.com', 'http://www-uk.hpl.hp.com/people/afs/fhomepage', NULL, 'http://www-uk.hpl.hp.com/people#andy_seaborne', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (9, 'Employee', 'Alberto', 'Reggiori', NULL, 'areggiori@webweaving.org', 'http://reggiori.webweaving.org', NULL, 'http://reggiori.webweaving.org#Alberto Reggiori', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (10, 'Professor', 'Sonia', 'Bergamaschi', 'DII- Universita di Modena e Reggio Emilia via Vignolese 905 41100 Modena', 'bergamaschi.sonia@unimo.it', 'http://www.dbgroup.unimo.it/Bergamaschi.html', '+39 059 2056132', 'http://www.dbgroup.unimo.it/iswc/iswc.html#S. Bergamaschi', NULL);
INSERT INTO `persons` (`PerID`, `Type`, `FirstName`, `LastName`, `Address`, `Email`, `Homepage`, `Phone`, `URI`, `Photo`) VALUES (11, 'Employee', 'Francesco', 'Guerra', 'DII- Universita di Modena e Reggio Emilia via Vignolese 905 41100 Modena Italy', 'guerra.francesco@unimo.it', 'http://www.dbgroup.unimo.it/~guerra/', '+39 059 20561543', 'http://www.dbgroup.unimo.it/iswc/iswc.html#F. Guerra', NULL);

# --------------------------------------------------------

#
# Table structure for table `rel_paper_topic`
#

CREATE TABLE `rel_paper_topic` (
  `PaperID` int(11) NOT NULL default '0',
  `TopicID` int(11) NOT NULL default '0',
  `RelationType` int(11) default NULL,
  PRIMARY KEY  (`PaperID`,`TopicID`)
) TYPE=InnoDB;

#
# Dumping data for table `rel_paper_topic`
#

INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (1, 5, 1);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (1, 15, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (2, 3, 3);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (2, 5, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (4, 10, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (4, 11, 1);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (4, 14, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (5, 2, 3);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (5, 3, 1);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (5, 13, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (6, 5, 2);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (6, 11, 1);
INSERT INTO `rel_paper_topic` (`PaperID`, `TopicID`, `RelationType`) VALUES (6, 13, 3);

# --------------------------------------------------------

#
# Table structure for table `rel_person_organization`
#

CREATE TABLE `rel_person_organization` (
  `PersonID` int(11) NOT NULL default '0',
  `OrganizationID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`OrganizationID`)
) TYPE=InnoDB;

#
# Dumping data for table `rel_person_organization`
#

INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (1, 1);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (2, 1);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (3, 1);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (4, 3);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (5, 5);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (5, 6);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (6, 7);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (9, 8);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (10, 9);
INSERT INTO `rel_person_organization` (`PersonID`, `OrganizationID`) VALUES (11, 9);

# --------------------------------------------------------

#
# Table structure for table `rel_person_paper`
#

CREATE TABLE `rel_person_paper` (
  `PersonID` int(11) NOT NULL default '0',
  `PaperID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`PaperID`)
) TYPE=InnoDB;

#
# Dumping data for table `rel_person_paper`
#

INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (1, 1);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (2, 1);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (4, 2);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (5, 6);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (6, 4);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (9, 4);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (10, 5);
INSERT INTO `rel_person_paper` (`PersonID`, `PaperID`) VALUES (11, 5);

# --------------------------------------------------------

#
# Table structure for table `rel_person_topic`
#

CREATE TABLE `rel_person_topic` (
  `PersonID` int(11) NOT NULL default '0',
  `TopicID` int(11) NOT NULL default '0',
  PRIMARY KEY  (`PersonID`,`TopicID`)
) TYPE=InnoDB;

#
# Dumping data for table `rel_person_topic`
#

INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (1, 1);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (1, 2);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (1, 3);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (2, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (2, 6);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (2, 7);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (2, 8);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (3, 1);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (3, 2);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (3, 3);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (4, 1);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (5, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (5, 7);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (5, 13);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (6, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (9, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (10, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (10, 10);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (10, 15);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (11, 5);
INSERT INTO `rel_person_topic` (`PersonID`, `TopicID`) VALUES (11, 10);

# --------------------------------------------------------

#
# Table structure for table `topics`
#

CREATE TABLE `topics` (
  `TopicID` int(11) NOT NULL default '0',
  `TopicName` varchar(50) default NULL,
  `URI` varchar(200) default NULL,
  PRIMARY KEY  (`TopicID`)
) TYPE=InnoDB;

#
# Dumping data for table `topics`
#

INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (1, 'Knowledge Representation Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Representation_Languages');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (2, 'Knowledge Systems', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Systems');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (3, 'Artificial Intelligence', 'http://annotation.semanticweb.org/iswc/iswc.daml#Artificial_Intelligence');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (4, 'Semantic Annotation', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Annotation');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (5, 'Semantic Web', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (6, 'Semantic Web Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web_Languages');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (7, 'Web Services', 'http://annotation.semanticweb.org/iswc/iswc.daml#Web_Services');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (8, 'World Wide Web', 'http://annotation.semanticweb.org/iswc/iswc.daml#World_Wide_Web');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (9, 'Text Mining', 'http://annotation.semanticweb.org/iswc/iswc.daml#Text_Mining');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (10, 'Databases', 'http://annotation.semanticweb.org/iswc/iswc.daml#Databases');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (11, 'Semantic Web Infrastructure', 'http://annotation.semanticweb.org/iswc/iswc.daml#Semantic_Web_Iinfrastructure');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (12, 'XML', 'http://annotation.semanticweb.org/iswc/iswc.daml#XML');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (13, 'E-Business', 'http://annotation.semanticweb.org/iswc/iswc.daml#e-Business');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (14, 'Query Languages', 'http://annotation.semanticweb.org/iswc/iswc.daml#Query_Languages');
INSERT INTO `topics` (`TopicID`, `TopicName`, `URI`) VALUES (15, 'Knowledge Management', 'http://annotation.semanticweb.org/iswc/iswc.daml#Knowledge_Management');

# Foreign key constraints
ALTER TABLE `papers` ADD FOREIGN KEY (`Conference`) REFERENCES `conferences` (`ConfID`);
ALTER TABLE `organizations` ADD FOREIGN KEY (`Belongsto`) REFERENCES `organizations` (`OrgID`);
ALTER TABLE `rel_paper_topic` ADD FOREIGN KEY (`PaperID`) REFERENCES `papers` (`PaperID`);
ALTER TABLE `rel_paper_topic` ADD FOREIGN KEY (`TopicID`) REFERENCES `topics` (`TopicID`);
ALTER TABLE `rel_person_organization` ADD FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`);
ALTER TABLE `rel_person_organization` ADD FOREIGN KEY (`OrganizationID`) REFERENCES `organizations` (`OrgID`);
ALTER TABLE `rel_person_paper` ADD FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`);
ALTER TABLE `rel_person_paper` ADD FOREIGN KEY (`PaperID`) REFERENCES `papers` (`PaperID`);
ALTER TABLE `rel_person_topic` ADD FOREIGN KEY (`PersonID`) REFERENCES `persons` (`PerID`);
ALTER TABLE `rel_person_topic` ADD FOREIGN KEY (`TopicID`) REFERENCES `topics` (`TopicID`);
