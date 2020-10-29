/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

/**
 * An entity reference or &#x2018;mention&#x2019;,
 * a span of text referring to an entity.
 */
public class Mention {
	
	private final int pBegin;
	private final int pEnd;
	private final String pEntityId;
	
	/**
	 * Initializes a new instance of this class.
	 * 
	 * @param begin
	 * the character index at which the span of text
	 * referring to the entity starts
	 * (inclusive)
	 * 
	 * @param end
	 * the character index at which the span of text
	 * referring to the entity ends
	 * (exclusive)
	 * 
	 * @param entityId
	 * the ID of the entity being referred to;
	 * not {@code null}
	 */
	public Mention(final int begin, final int end, final String entityId) {
		pBegin = begin;
		pEnd = end;
		pEntityId = entityId;
	}
	
	/**
	 * Returns the (inclusive) character index
	 * at which the span of text referring to the entity starts
	 * 
	 * @return
	 * the (inclusive) character index
	 * at which the span of text referring to the entity starts
	 */
	public int getBegin() {
		return pBegin;
	}
	
	/**
	 * Returns the (exclusive) character index
	 * at which the span of text referring to the entity ends
	 * 
	 * @return
	 * the (exclusive) character index
	 * at which the span of text referring to the entity ends
	 */
	public int getEnd() {
		return pEnd;
	}
	
	/**
	 * Returns the ID of the entity being referred to
	 * 
	 * @return
	 * the ID of the entity being referred to
	 */
	public String getEntityId() {
		return pEntityId;
	}
	
}
