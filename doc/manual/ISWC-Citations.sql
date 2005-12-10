# 
# Database : `iswccitations`
# 

# --------------------------------------------------------

#
# Table structure for table `papers`
#

CREATE TABLE `citation` (
  `CitationID` int(11) NOT NULL default '0',
  `FromPaperID` int(11) NOT NULL default '0',
  `ToPaperID` int(11) NOT NULL default '0',
  `Content` mediumtext
) TYPE=MyISAM;

#
# Dumping data for table `citation`
#

INSERT INTO `citation` (`CitationID`, `FromPaperID`, `ToPaperID`, `Content`) VALUES (1, 1, 2, 'page 3');
INSERT INTO `citation` (`CitationID`, `FromPaperID`, `ToPaperID`, `Content`) VALUES (2, 1, 4, 'why?');
INSERT INTO `citation` (`CitationID`, `FromPaperID`, `ToPaperID`, `Content`) VALUES (3, 2, 5, 'friend of 5');
INSERT INTO `citation` (`CitationID`, `FromPaperID`, `ToPaperID`, `Content`) VALUES (4, 5, 2, 'friend of 3');

