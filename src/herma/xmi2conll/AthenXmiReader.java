/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class AthenXmiReader extends DefaultHandler implements XmiReader {
	
	private final ArrayList<Mention> pMentions;
	private final HashMap<String, HashMap<String, Long>> pEntities;
	
	private String pDocumentText;
	
	private int pLevel;
	
	public AthenXmiReader() {
		pMentions = new ArrayList<>();
		pEntities = new HashMap<>();
		
		pDocumentText = null;
		
		pLevel = 0;
	}
	
	@Override
	public void attachToReader(final XMLReader xmlReader) {
		xmlReader.setContentHandler(this);
	}
	
	@Override
	public String getDocumentText() {
		return pDocumentText;
	}
	
	@Override
	public ArrayList<Entity> getEntities() {
		final ArrayList<Entity> entities = new ArrayList<>();
		for (final Entry<String, HashMap<String, Long>> entityEntry : pEntities.entrySet()) {
			final String id = entityEntry.getKey();
			final HashMap<String, Long> names = entityEntry.getValue();
			if (names.isEmpty()) {
				System.out.print("Warning: named entity ");
				System.out.print(id);
				System.out.println(" does not have a name");
			}
			final String mostFrequentName = names.entrySet().stream().max((x, y) -> x.getValue().compareTo(y.getValue())).map(Entry::getKey).orElse("?");
			entities.add(new Entity(id, mostFrequentName, null));
		}
		return entities;
	}
	
	@Override
	public ArrayList<Mention> getMentions() {
		return pMentions;
	}
	
	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		if (pLevel == 1)
			readElement(qName, attributes);
		pLevel++;
	}
	
	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		pLevel--;
	}
	
	private void readElement(final String qName, final Attributes attributes) {
		switch (qName) {
		case "type:NamedEntity":
			readNamedEntity(attributes);
			break;
		case "cas:Sofa":
			readSofa(attributes);
			break;
		}
	}
	
	private void readNamedEntity(final Attributes attributes) {
		final String id = attributes.getValue("ID");
		if (id == null) {
			System.out.println("Warning: skipping named entity without ID");
			return;
		}
		final String beginString = attributes.getValue("begin");
		final String endString = attributes.getValue("end");
		if ((beginString == null) || (endString == null)) {
			System.out.print("Warning: skipping named entity ");
			System.out.print(id);
			System.out.println(" without begin and/or end");
			return;
		}
		final int begin, end;
		try {
			begin = Integer.parseInt(beginString);
		} catch (final NumberFormatException e) {
			printNamedEntityBeginEndParseWarning(id, "begin", beginString);
			return;
		}
		try {
			end = Integer.parseInt(endString);
		} catch (final NumberFormatException e) {
			printNamedEntityBeginEndParseWarning(id, "end", beginString);
			return;
		}
		pMentions.add(new Mention(begin, end, id));
		countNamedEntityName(attributes, id);
	}
	
	private void countNamedEntityName(final Attributes attributes, final String id) {
		final HashMap<String, Long> entity = CollectionUtil.getSubmap(pEntities, id);
		final String entityString = attributes.getValue("Name");
		if (entityString != null)
			CollectionUtil.increment(entity, entityString);
	}
	
	private static void printNamedEntityBeginEndParseWarning(final String id, final String type, final String string) {
		System.out.print("Warning: " + type + ' ');
		System.out.print(string);
		System.out.print(" of named entity ");
		System.out.print(id);
		System.out.println(" cannot be parsed as a number, skipping this named entity");
	}
	
	private void readSofa(final Attributes attributes) {
		if (pDocumentText == null)
			pDocumentText = attributes.getValue("sofaString");
	}
	
}
