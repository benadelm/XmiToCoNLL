/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class for the alignment of a tokenization with a text.
 * Instances of this class are initialized with the text
 * which to align a tokenization with,
 * and then successively delivers the character indices
 * of tokens found in the text.
 * <p>
 * Schematically, the usage of this class should look like this:
 * </p>
 * <pre>Aligner aligner = new Aligner(text);
 *for (String tokenText : tokenization) {
 *	if (aligner.findNextToken()) {
 *		// check whether there is actually the expected token in the text
 *		if (text.startsWith(tokenText, aligner.getTokenStart())) {
 *			aligner.passToken(tokenText.length());
 *		} else {
 *			// error, not the expected token in the text
 *		}
 *	} else {
 *		// only whitespace remaining in the text
 *		// error, token not found
 *	}
 *}</pre>
 *
 */
public class Aligner {
	
	private static final Pattern SPACE_PATTERN = Pattern.compile("[\\p{Z}\t-\r\u001C-\u001F]*");
	
	private final Matcher pMatcher;
	private final int pEnd;
	private int pPos = 0;
	
	/**
	 * Initializes a new instance of this class
	 * with the text which to align a tokenization with.
	 * @param text
	 * the text which to align a tokenization with,
	 * not {@code null}
	 */
	public Aligner(final String text) {
		pMatcher = SPACE_PATTERN.matcher(text);
		pEnd = text.length();
	}
	
	/**
	 * Attempts to find the next token in the text
	 * by skipping an arbitrary amount of whitespace
	 * (possibly none).
	 * <p>
	 * If there is any character left in the string
	 * after skipping the whitespace,
	 * this method returns {@code true} and
	 * a subsequent call to
	 * {@link #getTokenStart()}
	 * returns the character index of the first code point
	 * after the whitespace.
	 * Otherwise, this method returns {@code false}
	 * and a subsequent call to
	 * {@link #getTokenStart()}
	 * returns the same value as before the call to this method.
	 * </p>
	 * 
	 * @return
	 * {@code true} if there is a next token in the text;
	 * {@code false} otherwise
	 */
	public boolean findNextToken() {
		if (pMatcher.find(pPos)) {
			final int whitespaceEnd = pMatcher.end();
			if (whitespaceEnd < pEnd) {
				pPos = whitespaceEnd;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the character index of the current position
	 * within the text being aligned with a tokenization.
	 * Between a call to
	 * {@link #findNextToken()}
	 * that returned {@code true}
	 * and the next call to
	 * {@link #findNextToken()}
	 * or
	 * {@link #passToken(int)},
	 * the position returned by this method
	 * is the character index of the start of the current token.
	 * 
	 * @return
	 * the character index of the current position within the text
	 */
	public int getTokenStart() {
		return pPos;
	}
	
	/**
	 * Passes over a token of the specified length (in characters).
	 * <p>
	 * Call this method to consume a token before attempting
	 * to find the next one using
	 * {@link #findNextToken()}.
	 * </p>
	 * <p>
	 * As the parameter of this method you should not pass a number
	 * that is greater than the number of remaining characters
	 * in the text.
	 * </p>
	 * 
	 * @param tokenLength
	 * the length, in the text, of the token to be passed;
	 * in characters
	 */
	public void passToken(final int tokenLength) {
		pPos += tokenLength;
	}
	
}
