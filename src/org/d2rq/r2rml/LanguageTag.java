package org.d2rq.r2rml;


public class LanguageTag extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @result <code>null</code> if arg is <code>null</code>
	 */
	public static LanguageTag create(String tag) {
		return tag == null ? null : new LanguageTag(tag);
	}

	private final String tag;

	private LanguageTag(String tag) {
		this.tag = tag.toLowerCase();
	}
	
	@Override
	public String toString() {
		return tag;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}

	public boolean equals(Object other) {
		if (!(other instanceof LanguageTag)) return false;
		return tag.equals(((LanguageTag) other).tag);
	}
	
	public int hashCode() {
		return tag.hashCode() ^ 29673;
	}
}
