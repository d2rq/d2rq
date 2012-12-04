package org.d2rq.r2rml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.r2rml.LanguageTag;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.validation.Message.Problem;
import org.junit.Test;



public class LanguageTagTest {

	@Test public void valid() {
		assertTrue(LanguageTag.create("en-US").isValid());
	}
	
	@Test public void normalizeLowerCase() {
		assertEquals("en-us", LanguageTag.create("en-US").toString());
	}
	
	@Test public void invalid() {
		assertFalse(LanguageTag.create("en us").isValid());
	}
	
	@Test public void validate() {
		MappingValidator validator = new MappingValidator(null);
		LanguageTag.create("en us").accept(validator);
		assertTrue(validator.getReport().hasError());
		assertEquals(Problem.INVALID_LANGUAGE_TAG, 
				validator.getReport().getMessages().get(0).getProblem());
	}
}
