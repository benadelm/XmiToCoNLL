/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.ArrayList;

import org.xml.sax.XMLReader;

/**
 * Can load coreference data from XMI files
 * (read using an {@link XMLReader}).
 * <p>
 * Intended usage:
 * After obtaining an {@link XMLReader} instance, pass it to
 * {@link #attachToReader(XMLReader)},
 * then call the
 * {@link XMLReader#parse(org.xml.sax.InputSource)}
 * method of the {@link XMLReader} for the XMI input data.
 * When that method has returned, coreference data
 * read from the XMI can be obtained using the other
 * methods of this class.
 * </p>
 *
 */
public interface XmiReader {
	
	/**
	 * Attaches this XMI reader to an {@link XMLReader}.
	 * 
	 * @param xmlReader
	 * the {@link XMLReader} to attach this XMI reader to;
	 * not {@code null}
	 */
	void attachToReader(XMLReader xmlReader);
	
	/**
	 * Returns the document text read from the XMI.
	 * 
	 * @return
	 * the document text read from the XMI;
	 * or {@code null} if no document text could be found
	 */
	String getDocumentText();
	
	/**
	 * Returns an {@link ArrayList} of
	 * {@link Entity}
	 * instances representing the different entities
	 * present in the XMI.
	 * 
	 * @return
	 * an {@link ArrayList} of
	 * {@link Entity}
	 * instances representing the different entities
	 * present in the XMI;
	 * not {@code null},
	 * and no item in the list will be {@code null}
	 */
	ArrayList<Entity> getEntities();
	
	/**
	 * Returns an {@link ArrayList} of
	 * {@link Mention}
	 * instances representing the entity references
	 * present in the XMI.
	 * 
	 * @return
	 * an {@link ArrayList} of
	 * {@link Mention}
	 * instances representing the entity references
	 * present in the XMI;
	 * not {@code null},
	 * and no item in the list will be {@code null}
	 */
	ArrayList<Mention> getMentions();
	
}
