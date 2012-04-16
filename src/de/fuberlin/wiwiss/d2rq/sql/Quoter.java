package de.fuberlin.wiwiss.d2rq.sql;

import java.util.regex.Pattern;

public interface Quoter {

	public abstract String quote(String s);

	public static class PatternDoublingQuoter implements Quoter {
		private final Pattern pattern;
		private final String quote;
		public PatternDoublingQuoter(Pattern pattern, String quote) {
			this.pattern = pattern;
			this.quote = quote;
		}
		public String quote(String s) {
			return quote + pattern.matcher(s).replaceAll("$1$1") + quote;
		}
	};
}
