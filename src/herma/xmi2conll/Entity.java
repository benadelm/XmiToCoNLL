/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.HashSet;

/**
 * An entity or entity group.
 */
public class Entity {

	private final String pId;
	private final String pLabel;
	private final HashSet<String> pSubEntityIds;
	
	/**
	 * Initializes a new instance of this class.
	 * 
	 * @param id
	 * the entity ID;
	 * not {@code null}
	 * 
	 * @param label
	 * the entity label;
	 * not {@code null}
	 * 
	 * @param subEntityIds
	 * the IDs of other entities subsumed under this entity as a group;
	 * {@code null} if this entity is not an entity group
	 */
	public Entity(final String id, final String label, final HashSet<String> subEntityIds) {
		pId = id;
		pLabel = label;
		pSubEntityIds = subEntityIds;
	}
	
	/**
	 * Returns the entity ID.
	 * 
	 * @return
	 * the entity ID;
	 * not {@code null}
	 */
	public String getId() {
		return pId;
	}
	
	/**
	 * Returns the entity label.
	 * 
	 * @return
	 * the entity label;
	 * not {@code null}
	 */
	public String getLabel() {
		return pLabel;
	}
	
	/**
	 * Returns the IDs of other entities
	 * subsumed under this entity as a group.
	 * 
	 * @return
	 * the IDs of other entities subsumed under this entity as a group;
	 * {@code null} if this entity is not an entity group
	 */
	public HashSet<String> getSubEntityIds() {
		return pSubEntityIds;
	}
	
}
