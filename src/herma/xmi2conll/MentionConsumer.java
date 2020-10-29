/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

/**
 * Can consume mentions (entity references) assigned to a token.
 *
 * @param <E>
 * the type of exceptions
 * that may be thrown by the methods of this interface
 */
public interface MentionConsumer<E extends Throwable> {
	
	/**
	 * Initializes the process of consuming mentions for a token.
	 * <p>
	 * This method has to be called before any call to
	 * any other method of this interface.
	 * After a call to
	 * {@link #endAppendMentions()},
	 * this method has to be called again
	 * before calling any other method of this interface.
	 * </p>
	 * 
	 * @throws E
	 * if trying to start appending mentions raises some error
	 */
	void startAppendMentions() throws E;
	
	/**
	 * Consumes a mention that includes only this single token.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws E
	 * if trying to consume the mention raises some error
	 */
	void openAndCloseMention(String entityId) throws E;
	
	/**
	 * Consumes a mention that starts with this token,
	 * but ends with some later one.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws E
	 * if trying to consume the mention raises some error
	 */
	void openMention(String entityId) throws E;
	
	/**
	 * Consumes a mention that started with some earlier token
	 * and ends with with this one.
	 * 
	 * @param entityId
	 * the entity id of the mention;
	 * not {@code null}
	 * 
	 * @throws E
	 * if trying to consume the mention raises some error
	 */
	void closeMention(String entityId) throws E;
	
	/**
	 * Terminates the process of consuming mentions for a token.
	 * <p>
	 * This method has to be called after all the calls for a single token
	 * to any other method of this interface.
	 * After it has been called,
	 * {@link #startAppendMentions()}
	 * has to be called before calling any method of this interface
	 * for the next token.
	 * </p>
	 * 
	 * @throws E
	 * if trying to end appending mentions raises some error
	 */
	void endAppendMentions() throws E;
	
}
