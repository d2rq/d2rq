package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.mapgen.Filter.IdentifierMatcher;

public class FilterParser {
	private final String s;
	private final List<List<IdentifierMatcher>> result = 
		new ArrayList<List<IdentifierMatcher>>();
	private int index = 0;
	
	public FilterParser(String filterSpec) {
		s = filterSpec;
	}
	
	public Filter parseSchemaFilter() throws ParseException {
		List<Filter> result = new ArrayList<Filter>();
		for (List<IdentifierMatcher> list: parse()) {
			if (list.size() != 1) {
				throw new ParseException("Syntax error in schema filter list; expected list of comma- or newline-separated schema names: '" + s + "'");
			}
			result.add(new FilterMatchSchema(list.get(0)));
		}
		return FilterMatchAny.create(result);
	}
	
	public Filter parseTableFilter(boolean matchParents) throws ParseException {
		List<Filter> result = new ArrayList<Filter>();
		for (List<IdentifierMatcher> list: parse()) {
			if (list.size() < 1 || list.size() > 2) {
				throw new ParseException("Syntax error in table filter list; expected list of comma- or newline-separated names in [schema.]table notation: '" + s + "'");
			}
			if (list.size() == 1) {
				result.add(new FilterMatchTable(Filter.NULL_MATCHER, list.get(0), matchParents));
			} else {
				result.add(new FilterMatchTable(list.get(0), list.get(1), matchParents));
			}
		}
		return FilterMatchAny.create(result);
	}
	
	public Filter parseColumnFilter(boolean matchParents) throws ParseException {
		List<Filter> result = new ArrayList<Filter>();
		for (List<IdentifierMatcher> list: parse()) {
			if (list.size() < 2 || list.size() > 3) {
				throw new ParseException("Syntax error in column filter list; expected list of comma- or newline-separated names in [schema.]table.column notation: '" + s + "'");
			}
			if (list.size() == 2) {
				result.add(new FilterMatchColumn(Filter.NULL_MATCHER, list.get(0), list.get(1), matchParents));
			} else {
				result.add(new FilterMatchColumn(list.get(0), list.get(1), list.get(2), matchParents));
			}
		}
		return FilterMatchAny.create(result);
	}
	
	public List<List<IdentifierMatcher>> parse() throws ParseException {
		eatSeparators();
		while (!atEnd()) {
			List<IdentifierMatcher> list = new ArrayList<IdentifierMatcher>();
			while (!atEnd()) {
				if (current() == '/') {
					list.add(parseRegex());
				} else {
					list.add(parseIdentifier());
				}
				if (!atEnd() && atFilterTerminator()) {
					break;
				}
				index++;	// skip dot
			}
			result.add(list);
			eatSeparators();
		}
		return result;
	}
	
	private void eatSeparators() {
		while (!atEnd() && atSeparator()) index++;
	}

	private char current() {
		return s.charAt(index);
	}

	private boolean atSeparator() {
		return current() == ' ' || current() == '\n' || current() == '\r'
			|| current() == '\t' || current() == ',';
	}
	
	private boolean atFilterTerminator() {
		return current() == ',' || current() == '\n' || current() == '\r';
	}
	
	private boolean inIdentifier() {
		return current() != '.' && current() != ',' && current() != '\n' && current() != '\r' && current() != '\t';
	}
	
	private boolean inRegex() {
		return current() != '/' && current() != '\n' && current() != '\r';
	}
	
	private boolean inFlags() {
		return current() == 'i' || current() == ' ' || current() == '\t';
	}
	
	private boolean atEnd() {
		return index >= s.length();		
	}
	
	private IdentifierMatcher parseIdentifier() {
		StringBuilder builder = new StringBuilder();
		while (!atEnd() && inIdentifier()) {
			builder.append(current());
			index++;
		}
		return Filter.createStringMatcher(builder.toString().trim());
	}
	
	private IdentifierMatcher parseRegex() throws ParseException {
		StringBuilder builder = new StringBuilder();
		index++;	// skip initial '/'
		while (!atEnd() && inRegex()) {
			builder.append(current());
			index++;
		}
		if (atEnd() || current() != '/') throw new ParseException("Unterminated regex: /" + builder.toString());
		index++;	// skip final '/'
		int flags = 0;
		while (!atEnd() && inFlags()) {
			if (current() == 'i') {
				flags |= Pattern.CASE_INSENSITIVE;
			}
			index++;
		}
		return Filter.createPatternMatcher(Pattern.compile(builder.toString(), flags));
	}
	
	public class ParseException extends Exception {
		public ParseException(String message) {
			super(message);
		}
	}
}
