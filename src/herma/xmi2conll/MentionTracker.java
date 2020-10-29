/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * A class for tracking mentions (entity references)
 * hitting token spans in the text.
 * <p>
 * Given a sequence of tokens
 * that appear after each other in a text (without overlap),
 * an instance of this class keeps track of the mentions
 * that are &#x2018;open&#x2019; (hit the span of the current token)
 * or have to be closed, and calls appropriate methods of a
 * {@link MentionConsumer}
 * for those mentions newly opened and/or closed.
 * </p>
 * <p>
 * Mentions are always closed at sentence boundaries.
 * If a mention does not only hit tokens in one sentence
 * but also in the next sentence
 * (&#x2018;crosses a sentence boundary&#x2019;),
 * it is re-opened upon the first token of that next sentence.
 * </p>
 * <p>
 * Therefore, and because it can in general not be determined
 * whether a mention ends with a token or extends further
 * until the <i>next</i> token is processed,
 * the flow of using an instance of this class is a bit complicated.
 * For each sentence:
 * </p>
 * <ol>
 * <li>Call
 * {@link #startSentence(int, int)}
 * with start and end of the span
 * of the first token in the sentence;</li>
 * <li>call
 * {@link #determinePreviousTokenMentions(int, int, MentionConsumer)}
 * with start and end of the span of any <i>subsequent</i> token
 * in the sentence, in order, with a
 * {@link MentionConsumer}
 * to consume the mentions belonging to the respective
 * <i>previous</i> token;</li>
 * <li>call
 * {@link #endSentence(MentionConsumer)}
 * at the end of the sentence with a
 * {@link MentionConsumer}
 * to consume the mentions belonging to the <i>last</i> token
 * of the sentence.</li>
 * </ol>
 * <p>
 * For example, a sentence containing the tokens
 * {@code a}, {@code b} and {@code c}
 * would require the following call pattern (pseudocode):
 * </p>
 * <pre>startSentence(a.start(), a.end());
 *determinePreviousTokenMentions(b.start(), b.end(), a.mentionConsumer());
 *determinePreviousTokenMentions(c.start(), c.end(), b.mentionConsumer());
 *endSentence(c.mentionConsumer())</pre>
 * <p>
 * A
 * {@link MessageGenerator}
 * is used to issue a warning
 * when a mention crosses a sentence boundary.
 * A warning is also issued if a mention does not hit any token
 * (and therefore never consumed by a
 * {@link MentionConsumer}).
 * </p>
 *
 */
public class MentionTracker {
	
	private final Iterator<? extends Mention> pMentions;
	
	private final MessageGenerator pWarningGenerator;
	
	private final ArrayDeque<Mention> pOpenMentions;
	
	private boolean pHasNextMention;
	private Mention pNextMention;
	
	private int pNewlyOpenedMentions;
	
	/**
	 * Initializes a new instance of this class.
	 * <p>
	 * <b>Note:</b> The provided list of mentions will be modified.
	 * Furthermore, this list must not be modified
	 * after this constructor returns.
	 * (Modifications are very likely to result in a
	 * {@link ConcurrentModificationException}
	 * being thrown in a method of this class.)
	 * </p>
	 * 
	 * @param mentions
	 * an {@link ArrayList} containing the mentions to be tracked;
	 * not {@code null}, and no item in it may be {@code null}
	 * 
	 * @param warningGenerator
	 * the
	 * {@link MessageGenerator}
	 * to be used for issuing warnings;
	 * not {@code null}
	 */
	public MentionTracker(final ArrayList<? extends Mention> mentions, final MessageGenerator warningGenerator) {
		sortEntityReferencesByBegin(mentions);
		pMentions = mentions.iterator();
		
		pWarningGenerator = warningGenerator;
		
		pOpenMentions = new ArrayDeque<>();
		
		pHasNextMention = pMentions.hasNext();
		if (pHasNextMention)
			pNextMention = pMentions.next();
	}
	
	private static void sortEntityReferencesByBegin(final ArrayList<? extends Mention> entityReferences) {
		Collections.sort(entityReferences, (m1, m2) -> Integer.compare(m1.getBegin(), m2.getBegin()));
	}
	
	/**
	 * To be called at the beginning of a sentence.
	 * 
	 * @param tokenStart
	 * the start index of the span of the first token in the sentence
	 * 
	 * @param tokenEnd
	 * the end index of the span of the first token in the sentence
	 */
	public void startSentence(final int tokenStart, final int tokenEnd) {
		// load excess tokens from the previous sentence to be re-opened on the first token of this sentence
		int nOpenMentions = pOpenMentions.size();
		while (nOpenMentions > 0) {
			nOpenMentions--;
			final Mention mention = pOpenMentions.poll();
			if (canHitThisOrEarlierToken(mention.getEnd(), tokenStart, tokenEnd)) {
				pWarningGenerator.printMentionCrossesSentenceBoundaryWarning(mention);
				pOpenMentions.add(mention);
			}
		}
		loadMentionsOpenedAt(tokenStart, tokenEnd);
		pNewlyOpenedMentions = pOpenMentions.size();
	}
	
	/**
	 * To be called in order for the tokens in a sentence,
	 * except the first token.
	 * <p>
	 * This method also takes a
	 * {@link MentionConsumer}
	 * to consume the mentions belonging to the
	 * <i>previous</i> token.
	 * {@link MentionConsumer#startAppendMentions()}
	 * is called, then
	 * {@link MentionConsumer#openMention(String)},
	 * {@link MentionConsumer#closeMention(String)}
	 * and/or
	 * {@link MentionConsumer#openAndCloseMention(String)}
	 * zero or more times,
	 * for the mentions belonging to the previous token,
	 * and finally
	 * {@link MentionConsumer#endAppendMentions()}
	 * is called. Any exceptions thrown by thos methods
	 * are relayed to the caller.
	 * </p>
	 * 
	 * @param <E>
	 * the type of exceptions that may be thrown by the provided
	 * {@link MentionConsumer}
	 * 
	 * @param tokenStart
	 * the start index of the span of the token
	 * 
	 * @param tokenEnd
	 * the end index of the span of the token
	 * 
	 * @param mentionConsumer
	 * a
	 * {@link MentionConsumer}
	 * to consume the mentions belonging to
	 * the <i>previous</i> token;
	 * not {@code null}
	 * 
	 * @throws E
	 * if thrown by a method of the
	 * {@link MentionConsumer}
	 */
	public <E extends Throwable> void determinePreviousTokenMentions(final int tokenStart, final int tokenEnd, final MentionConsumer<E> mentionConsumer) throws E {
		final int remainingMentions = pOpenMentions.size();
		
		loadMentionsOpenedAt(tokenStart, tokenEnd);
		final int newlyOpenedMentions = pOpenMentions.size() - remainingMentions;
		
		mentionConsumer.startAppendMentions();
		
		int i = 0;
		while (i < pNewlyOpenedMentions) {
			i++;
			final Mention mention = pOpenMentions.poll();
			if (canHitThisOrEarlierToken(mention.getEnd(), tokenStart, tokenEnd)) {
				mentionConsumer.openMention(mention.getEntityId());
				pOpenMentions.add(mention);
			} else {
				mentionConsumer.openAndCloseMention(mention.getEntityId());
			}
		}
		
		while (i < remainingMentions) {
			i++;
			final Mention mention = pOpenMentions.poll();
			if (canHitThisOrEarlierToken(mention.getEnd(), tokenStart, tokenEnd)) {
				pOpenMentions.add(mention);
			} else {
				mentionConsumer.closeMention(mention.getEntityId());
			}
		}
		
		mentionConsumer.endAppendMentions();
		
		pNewlyOpenedMentions = newlyOpenedMentions;
	}
	
	private void loadMentionsOpenedAt(final int tokenStart, final int tokenEnd) {
		// determine mentions newly opened on the token with the specified span
		while (pHasNextMention && canHitThisOrLaterToken(pNextMention.getBegin(), tokenStart, tokenEnd)) {
			if (canHitThisOrEarlierToken(pNextMention.getEnd(), tokenStart, tokenEnd))
				pOpenMentions.add(pNextMention);
			else
				pWarningGenerator.printMentionSkippedWarning(pNextMention);
			if (pMentions.hasNext()) {
				pNextMention = pMentions.next();
			} else {
				pHasNextMention = false;
				break;
			}
		}
	}
	
	/**
	 * To be called at the end of a sentence.
	 * <p>
	 * This method also takes a
	 * {@link MentionConsumer}
	 * to consume the mentions belonging to the
	 * last token of the sentence.
	 * {@link MentionConsumer#startAppendMentions()}
	 * is called, then
	 * {@link MentionConsumer#openMention(String)},
	 * {@link MentionConsumer#closeMention(String)}
	 * and/or
	 * {@link MentionConsumer#openAndCloseMention(String)}
	 * zero or more times,
	 * for the mentions belonging to the last token,
	 * and finally
	 * {@link MentionConsumer#endAppendMentions()}
	 * is called. Any exceptions thrown by thos methods
	 * are relayed to the caller.
	 * </p>
	 * 
	 * @param <E>
	 * the type of exceptions that may be thrown by the provided
	 * {@link MentionConsumer}
	 * 
	 * @param mentionConsumer
	 * a
	 * {@link MentionConsumer}
	 * to consume the mentions belonging to
	 * the last token of the sentence;
	 * not {@code null}
	 * 
	 * @throws E
	 * if thrown by a method of the
	 * {@link MentionConsumer}
	 */
	public <E extends Throwable> void endSentence(final MentionConsumer<E> mentionConsumer) throws E {
		mentionConsumer.startAppendMentions();
		
		int i = 0;
		for (final Mention mention : pOpenMentions) {
			if (i < pNewlyOpenedMentions)
				mentionConsumer.openAndCloseMention(mention.getEntityId());
			else
				mentionConsumer.closeMention(mention.getEntityId());
			i++;
		}
		
		mentionConsumer.endAppendMentions();
	}
	
	/**
	 * Partial check for a mention hitting a token span:
	 * Determines whether a mention ends late enough
	 * to hit a token with the specified span.
	 * 
	 * @param mentionEnd
	 * the end index of the mention span
	 * 
	 * @param tokenStart
	 * the start index of the token span
	 * 
	 * @param tokenEnd
	 * the end index of the token span
	 * 
	 * @return
	 * {@code true} if the mention ends late enough
	 * to hit the specified token span;
	 * {@code false} otherwise
	 */
	private static boolean canHitThisOrEarlierToken(final int mentionEnd, final int tokenStart, final int tokenEnd) {
		return mentionEnd > tokenStart;
	}
	
	/**
	 * Partial check for a mention hitting a token span:
	 * Determines whether a mention starts early enough
	 * to hit a token with the specified span.
	 * 
	 * @param mentionStart
	 * the start index of the mention span
	 * 
	 * @param tokenStart
	 * the start index of the token span
	 * 
	 * @param tokenEnd
	 * the end index of the token span
	 * 
	 * @return
	 * {@code true} if the mention starts early enough
	 * to hit the specified token span;
	 * {@code false} otherwise
	 */
	private static boolean canHitThisOrLaterToken(final int mentionStart, final int tokenStart, final int tokenEnd) {
		return mentionStart < tokenEnd;
	}
	
}
