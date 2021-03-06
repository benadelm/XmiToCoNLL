/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.io.IOException;

/**
 * A class for writing tokens with mentions
 * in the CoNLL-2012 format to an {@link Appendable}.
 * <p>
 * This class implements
 * {@link MentionConsumer}
 * parameterized with {@link IOException}
 * to be able to pass on such exceptions possibly thrown
 * by the methods of {@link Appendable}.
 * </p>
 * <p>
 * This class generates output according to the CoNLL-2012 format
 * as described
 * <a href="http://conll.cemantix.org/2012/data.html">here</a>
 * (section &#x2018;<code>*_conll</code> File Format&#x2019;).
 * However, only columns one, three and four
 * (Document ID, Word number, Word itself)
 * as well as the coreference column ({@code N}) are filled
 * (the value to be used as Document ID
 * is specified in the constructor).
 * Colum two (Part number) is always set to {@code 0} (zero).
 * All other columns are set to {@code _} (underscore).
 * As there are no Predicate Arguments,
 * the coreference information resides in column twelve.
 * </p>
 * <p>
 * In the coreference column, mentions are encoded
 * as an unordered list, separated by {@code |} (pipe),
 * of their entity IDs.
 * Only mentions that start and/or end with the respective token
 * are included in the coreference column.
 * The entity IDs of mentions starting with the respective token
 * are preceded by an opening bracket,
 * those of mentions ending with the respective token
 * are followed by a closing bracket.
 * </p>
 * <ul>
 * <li>entity 123 starts at this token (and ends at a later one):
 * {@code (123}</li>
 * <li>entity 123 ends at this token (and started at an earlier one):
 * {@code 123)}</li>
 * <li>entity 123 comprises only this token (starts and ends here):
 * {@code (123)}</li>
 * </ul>
 * <p>
 * An example token line generated by this class may look like this:
 * </p>
 * <pre>document_name	0	8	token_text	_	_	_	_	_	_	_	(21508|(21557)</pre>
 * <p>
 * Additionally, this class creates comment lines at the beginning and end of the file:
 * </p>
 * <pre>#begin document (&lt;document name&gt;); part 0</pre>
 * <pre>#end document &lt;document name&gt;</pre>
 * <p>
 * This class takes care of sequentially numbering the tokens in a sentence,
 * starting with {@code 1} for the first token of each sentence.
 * </p>
 *
 */
public class CoNLL2012Writer implements MentionConsumer<IOException> {
	
	private final Appendable pAppendable;
	private final String pDocumentName;
	
	private int pRunningTokenIndex;
	private boolean pFirstMention;
	
	/**
	 * Initializes a new instance of this class.
	 * 
	 * @param appendable
	 * the {@link Appendable} to append the output to;
	 * not {@code null}
	 * 
	 * @param documentName
	 * the document name to be inserted into the first column
	 * of each token line;
	 * not {@code null}
	 * (and should not be an empty string)
	 */
	public CoNLL2012Writer(final Appendable appendable, final String documentName) {
		pAppendable = appendable;
		pDocumentName = documentName;
	}
	
	/**
	 * Initializes the output of CoNLL-12 data.
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	public void startDocument() throws IOException {
		pAppendable.append("#begin document (").append(pDocumentName).append("); part 0");
		pRunningTokenIndex = 0;
	}
	
	/**
	 * Outputs a token with the specified token text;
	 * a new token line is started and all columns
	 * except the coreference column are appended to
	 * the {@link Appendable} provided to the constructor.
	 * 
	 * @param tokenText
	 * the token text;
	 * not {@code null}
	 * (and should not be an empty string)
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	public void appendTokenText(final String tokenText) throws IOException {
		pRunningTokenIndex++;
		
		pAppendable.append('\n');
		pAppendable.append(pDocumentName);
		pAppendable.append("\t0\t");
		pAppendable.append(Integer.toString(pRunningTokenIndex));
		pAppendable.append('\t');
		pAppendable.append(tokenText);
		pAppendable.append("\t_\t_\t_\t_\t_\t_\t_\t");
	}
	
	/**
	 * Outputs a sentence boundary (empty line).
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	public void insertSentenceBoundary() throws IOException {
		pRunningTokenIndex = 0;
		pAppendable.append('\n');
	}
	
	/**
	 * Terminates the output of CoNLL-12 data.
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	public void endDocument() throws IOException {
		pAppendable.append("\n#end document ").append(pDocumentName);
	}
	
	/**
	 * Starts appending mentions to the coreference column
	 * of the current token.
	 * <p>
	 * This method has to be called after a call to
	 * {@link #appendTokenText(String)}
	 * and before any call to
	 * {@link #openMention(String)},
	 * {@link #closeMention(String)},
	 * {@link #openAndCloseMention(String)}
	 * or
	 * {@link #endAppendMentions()}
	 * and before the next call to
	 * {@link #appendTokenText(String)}
	 * or
	 * {@link #insertSentenceBoundary()}.
	 * </p>
	 */
	@Override
	public void startAppendMentions() {
		pFirstMention = true;
	}
	
	/**
	 * Appends to the coreference column of the current token
	 * a mention that includes only this token.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	@Override
	public void openAndCloseMention(final String entityId) throws IOException {
		appendMention(entityId, true, true);
	}
	
	/**
	 * Appends to the coreference column of the current token
	 * a mention that starts with this token,
	 * but ends with some later one.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	@Override
	public void openMention(final String entityId) throws IOException {
		appendMention(entityId, true, false);
	}
	
	/**
	 * Appends to the coreference column of the current token
	 * a mention that started with some earlier token
	 * and ends with with this one.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	@Override
	public void closeMention(final String entityId) throws IOException {
		appendMention(entityId, false, true);
	}
	
	private void appendMention(final String entityId, final boolean startsHere, final boolean endsHere) throws IOException {
		if (pFirstMention)
			pFirstMention = false;
		else
			pAppendable.append('|');
		if (startsHere)
			pAppendable.append('(');
		pAppendable.append(entityId);
		if (endsHere)
			pAppendable.append(')');
	}
	
	/**
	 * Finishes appending mentions to the coreference column
	 * of the current token.
	 * If no mention has been appended, the column is filled
	 * with an underscore ({@code _}).
	 * <p>
	 * This method has to be called after all the calls to
	 * {@link #startAppendMentions()},
	 * {@link #openMention(String)},
	 * {@link #closeMention(String)}
	 * and
	 * {@link #openAndCloseMention(String)}
	 * for the current token.
	 * After it has been called,
	 * {@link #startAppendMentions()}
	 * has to be called before calling
	 * {@link #openMention(String)},
	 * {@link #closeMention(String)}
	 * and
	 * {@link #openAndCloseMention(String)}
	 * again (for the next token).
	 * </p>
	 * 
	 * @throws IOException
	 * if thrown by a method of the {@link Appendable}
	 * provided to the constructor
	 */
	@Override
	public void endAppendMentions() throws IOException {
		if (pFirstMention)
			pAppendable.append('_');
	}
	
}
