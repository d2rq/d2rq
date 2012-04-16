package de.fuberlin.wiwiss.pubby.negotiation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ContentTypeNegotiator {
	private List<VariantSpec> variantSpecs = new ArrayList<VariantSpec>();
	private List<MediaRangeSpec> defaultAcceptRanges = 
		Collections.singletonList(MediaRangeSpec.parseRange("*/*"));
	private Collection<AcceptHeaderOverride> userAgentOverrides = new ArrayList<AcceptHeaderOverride>();
	
	public VariantSpec addVariant(String mediaType) {
		VariantSpec result = new VariantSpec(mediaType);
		variantSpecs.add(result);
		return result;
	}
	
	/**
	 * Sets an Accept header to be used as the default if a client does
	 * not send an Accept header, or if the Accept header cannot be parsed.
	 * Defaults to "* / *".
	 */
	public void setDefaultAccept(String accept) {
		this.defaultAcceptRanges = MediaRangeSpec.parseAccept(accept);
	}
	
	/**
	 * Overrides the Accept header for certain user agents. This can be
	 * used to implement special-case handling for user agents that send
	 * faulty Accept headers. 
	 * @param userAgentString A pattern to be matched against the User-Agent header;
	 * 		<tt>null</tt> means regardless of User-Agent
	 * @param originalAcceptHeader Only override the Accept header if the user agent
	 * 		sends this header; <tt>null</tt> means always override  
	 * @param newAcceptHeader The Accept header to be used instead
	 */
	public void addUserAgentOverride(Pattern userAgentString, 
			String originalAcceptHeader, String newAcceptHeader) {
		this.userAgentOverrides.add(new AcceptHeaderOverride(
				userAgentString, originalAcceptHeader, newAcceptHeader));
	}
	
	public MediaRangeSpec getBestMatch(String accept) {
		return getBestMatch(accept, null);
	}
	
	public MediaRangeSpec getBestMatch(String accept, String userAgent) {
		if (userAgent == null) {
			userAgent = "";
		}
		String overriddenAccept = accept;
		for (AcceptHeaderOverride override: userAgentOverrides) {
			if (override.matches(accept, userAgent)) {
				overriddenAccept = override.getReplacement();
			}
		}
		return new Negotiation(toAcceptRanges(overriddenAccept)).negotiate();
	}
	
	private List<MediaRangeSpec> toAcceptRanges(String accept) {
		if (accept == null) {
			return defaultAcceptRanges;
		}
		List<MediaRangeSpec> result = MediaRangeSpec.parseAccept(accept);
		if (result.isEmpty()) {
			return defaultAcceptRanges;
		}
		return result;
	}
	
	public class VariantSpec {
		private MediaRangeSpec type;
		private List<MediaRangeSpec> aliases = new ArrayList<MediaRangeSpec>();
		private boolean isDefault = false;
		public VariantSpec(String mediaType) {
			type = MediaRangeSpec.parseType(mediaType);
		}
		public VariantSpec addAliasMediaType(String mediaType) {
			aliases.add(MediaRangeSpec.parseType(mediaType));
			return this;
		}
		public void makeDefault() {
			isDefault = true;
		}
		public MediaRangeSpec getMediaType() {
			return type;
		}
		public boolean isDefault() {
			return isDefault;
		}
		public List<MediaRangeSpec> getAliases() {
			return aliases;
		}
	}
	
	private class Negotiation {
		private final List<MediaRangeSpec> ranges;
		private MediaRangeSpec bestMatchingVariant = null;
		private MediaRangeSpec bestDefaultVariant = null;
		private double bestMatchingQuality = 0;
		private double bestDefaultQuality = 0;
		
		Negotiation(List<MediaRangeSpec> ranges) {
			this.ranges = ranges;
		}
		
		MediaRangeSpec negotiate() {
			for (VariantSpec variant: variantSpecs) {
				if (variant.isDefault) {
					evaluateDefaultVariant(variant.getMediaType());
				}
				evaluateVariant(variant.getMediaType());
				for (MediaRangeSpec alias: variant.getAliases()) {
					evaluateVariantAlias(alias, variant.getMediaType());
				}
			}
			return (bestMatchingVariant == null) ? bestDefaultVariant : bestMatchingVariant;
		}
		
		private void evaluateVariantAlias(MediaRangeSpec variant, MediaRangeSpec isAliasFor) {
			if (variant.getBestMatch(ranges) == null) return;
			double q = variant.getBestMatch(ranges).getQuality();
			if (q * variant.getQuality() > bestMatchingQuality) {
				bestMatchingVariant = isAliasFor;
				bestMatchingQuality = q * variant.getQuality();
			}
		}
		
		private void evaluateVariant(MediaRangeSpec variant) {
			evaluateVariantAlias(variant, variant);
		}
		
		private void evaluateDefaultVariant(MediaRangeSpec variant) {
			if (variant.getQuality() > bestDefaultQuality) {
				bestDefaultVariant = variant;
				bestDefaultQuality = 0.00001 * variant.getQuality();
			}
		}
	}
	
	private class AcceptHeaderOverride {
		private Pattern userAgentPattern;
		private String original;
		private String replacement;
		AcceptHeaderOverride(Pattern userAgentPattern, String original, String replacement) {
			this.userAgentPattern = userAgentPattern;
			this.original = original;
			this.replacement = replacement;
		}
		boolean matches(String acceptHeader, String userAgentHeader) {
			return (userAgentPattern == null 
							|| userAgentPattern.matcher(userAgentHeader).find()) 
					&& (original == null || original.equals(acceptHeader));
		}
		String getReplacement() {
			return replacement;
		}
	}
}