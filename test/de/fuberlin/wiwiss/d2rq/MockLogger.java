/*
 * $Id: MockLogger.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * Mock implementation of the {@link Logger} class for unit testing. It only
 * checks for calls on the {@link Logger#warning} method.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class MockLogger extends Logger {
	private List expectedMessage = new ArrayList();

	public void expectWarning(String message) {
		this.expectedMessage.add(message);
	}
	
	public void warning(String message) {
		if (this.expectedMessage.isEmpty()) {
			Assert.fail("unexpected warning '" + message + "'");
		}
		String expected = (String) this.expectedMessage.remove(0);
		if (expected == null) {
			return;
		}
		Assert.assertEquals(expected, message);
	}
	
	public void tally() {
		Assert.assertTrue(this.expectedMessage.isEmpty());
	}
}