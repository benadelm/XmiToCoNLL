/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for generating messages for warnings and errors
 * that arise during the conversion.
 */
public class MessageGenerator {
	
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("(?:\\r\\n)|(?:\\n)|(?:\\r)");
	
	private final String pDocumentText;
	
	/**
	 * Initializes a new instance of this class
	 * with the specified document text.
	 * 
	 * @param documentText
	 * the document text;
	 * not {@code null}
	 */
	public MessageGenerator(final String documentText) {
		pDocumentText = documentText;
	}
	
	/**
	 * Prints a message to {@link System#err}
	 * stating that an expected token cannot be found
	 * in the document text
	 * because there is only whitespace left.
	 * 
	 * @param pos
	 * the location (character index) in the document text
	 * after which the token is expected, but only whitespace present
	 * 
	 * @param tokenText
	 * the token text of the expected token;
	 * not {@code null}
	 */
	public void printNoTokenErrorMessage(final int pos, final String tokenText) {
		System.err.println("The provided tokenization does not match the document text: expecting token, but there is only whitespace until the end of the text.");
		printPositionStandalone(System.err, pos);
		System.err.print("expected token: ");
		System.err.println(tokenText);
	}
	
	/**
	 * Prints a message to {@link System#err}
	 * stating that instead of an expected token
	 * other text was found in the document text.
	 * 
	 * @param pos
	 * the location (character index) in the document text
	 * where the token is expected but another string found
	 * 
	 * @param tokenText
	 * the token text of the expected token;
	 * not {@code null}
	 */
	public void printWrongTokenErrorMessage(final int pos, final String tokenText) {
		System.err.println("The provided tokenization does not match the document text: text and expected token deviate.");
		printPositionStandalone(System.err, pos);
		System.err.print("expected token: ");
		System.err.println(tokenText);
		System.err.print("there instead: ");
		final int tokenEnd = Math.min(pDocumentText.length(), pos + tokenText.length());
		System.err.println(pDocumentText.substring(pos, tokenEnd));
		System.err.println("more context:");
		System.err.println(moreContext(pos, tokenEnd));
	}
	
	public void printRemainingDocumentTextWarning(final int pos) {
		System.out.print("Warning: The remainder of the document text starting at ");
		printPosition(System.out, pos);
		System.out.println(" is not covered by the provided tokenization.");
	}
	
	/**
	 * Prints a message to {@link System#out}
	 * stating that a mention (entity reference)
	 * did not hit a token in the tokenization
	 * and was therefore not assigned to any,
	 * but &#x2018;skipped&#x2019.
	 * 
	 * @param mention
	 * the skipped mention (entity reference);
	 * not {@code null}
	 */
	public void printMentionSkippedWarning(final Mention mention) {
		System.out.print("Warning: mention of entity ");
		System.out.print(mention.getEntityId());
		System.out.print(' ');
		printRange(System.out, mention.getBegin(), mention.getEnd());
		System.out.println(" does not hit any token in the provided tokenization.");
	}
	
	/**
	 * Prints a message to {@link System#out}
	 * stating that a mention (entity reference)
	 * crosses a sentence boundary.
	 * 
	 * @param mention
	 * the mention (entity reference) that crosses a sentence boundary;
	 * not {@code null}
	 */
	public void printMentionCrossesSentenceBoundaryWarning(final Mention mention) {
		System.out.print("Warning: mention of entity ");
		System.out.print(mention.getEntityId());
		System.out.print(' ');
		printRange(System.out, mention.getBegin(), mention.getEnd());
		System.out.println(" crosses a sentence boundary in the provided tokenization.");
	}
	
	private void printPositionStandalone(final PrintStream stream, final int pos) {
		System.err.print("location: ");
		printPosition(stream, pos);
		System.err.println();
	}
	
	private void printRange(final PrintStream stream, final int from, final int to) {
		System.out.print("from ");
		printPosition(stream, from);
		System.out.print(" to ");
		printPosition(stream, to);
	}
	
	private void printPosition(final PrintStream stream, final int pos) {
		final Matcher m = NEWLINE_PATTERN.matcher(pDocumentText);
		int line = 1;
		int col = 0;
		while (m.find()) {
			final int end = m.end();
			if (end > pos)
				break;
			col = end;
			line++;
		}
		col = pDocumentText.codePointCount(col, pos) + 1;
		final int index = pDocumentText.codePointCount(0, pos);
		stream.print("index ");
		stream.print(Integer.toString(index));
		stream.print(" (l. ");
		stream.print(Integer.toString(line));
		stream.print(", c. ");
		stream.print(Integer.toString(col));
		stream.print(')');
	}
	
	private String moreContext(int start, int end) {
		for (int i = 0; i < 30; i++) {
			if (start == 0)
				break;
			start -= Character.charCount(pDocumentText.codePointBefore(start));
		}
		final int n = pDocumentText.length();
		for (int i = 0; i < 30; i++) {
			if (end < n)
				end += Character.charCount(pDocumentText.codePointAt(end));
			else
				break;
		}
		return pDocumentText.substring(start, end);
	}

}
