/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class CorefAnnotatorXmiReader extends DefaultHandler implements XmiReader {
	
	private static final Pattern SPACE_PATTERN = Pattern.compile("\\p{Z}+");
	
	private final ArrayList<Mention> pMentions;
	private final ArrayList<Entity> pEntities;
	
	private String pDocumentText;
	
	private int pLevel;
	
	public CorefAnnotatorXmiReader() {
		pMentions = new ArrayList<>();
		pEntities = new ArrayList<>();
		
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
		return pEntities;
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
		switch (noNamespace(qName)) {
		case "Mention":
			readMention(attributes);
			break;
		case "Entity":
			readEntity(attributes);
			break;
		case "EntityGroup":
			readEntityGroup(attributes);
			break;
		case "Sofa":
			readSofa(attributes);
			break;
		}
	}
	
	private void readMention(final Attributes attributes) {
		final String entity = attributes.getValue("Entity");
		if (entity == null) {
			System.out.println("Warning: skipping mention without entity");
			return;
		}
		final String beginString = attributes.getValue("begin");
		final String endString = attributes.getValue("end");
		if ((beginString == null) || (endString == null)) {
			System.out.println("Warning: skipping mention without begin and/or end");
			return;
		}
		final int begin, end;
		try {
			begin = Integer.parseInt(beginString);
		} catch (final NumberFormatException e) {
			printMentionBeginEndParseWarning("begin", beginString);
			return;
		}
		try {
			end = Integer.parseInt(endString);
		} catch (final NumberFormatException e) {
			printMentionBeginEndParseWarning("end", beginString);
			return;
		}
		pMentions.add(new Mention(begin, end, entity));
	}
	
	private static void printMentionBeginEndParseWarning(final String type, final String string) {
		System.out.print("Warning: mention " + type + ' ');
		System.out.print(string);
		System.out.println(" cannot be parsed as a number, skipping this mention");
	}
	
	private void readEntity(final Attributes attributes) {
		final String id = attributes.getValue("xmi:id");
		if (id == null) {
			printNoIdWarning("entity");
			return;
		}
		final String label = attributes.getValue("Label");
		if (label == null) {
			printNoLabelWarning("entity", id);
			return;
		}
		pEntities.add(new Entity(id, label, null));
	}
	
	private void readEntityGroup(final Attributes attributes) {
		final String id = attributes.getValue("xmi:id");
		if (id == null) {
			printNoIdWarning("entity group");
			return;
		}
		final String label = attributes.getValue("Label");
		if (label == null) {
			printNoLabelWarning("entity group", id);
			return;
		}
		final HashSet<String> members;
		final String membersString = attributes.getValue("Members");
		if (membersString == null) {
			System.out.print("Warning: entity group ");
			System.out.print(id);
			System.out.println(" does not have a member list, treating it as a non-group entity");
			members = null;
		} else {
			members = SPACE_PATTERN.splitAsStream(membersString).collect(Collectors.toCollection(HashSet::new));
			members.remove("");
			if (members.isEmpty()) {
				System.out.print("Warning: entity group ");
				System.out.print(id);
				System.out.println(" has an empty member list");
			}
		}
		pEntities.add(new Entity(id, label, members));
	}
	
	private static void printNoIdWarning(final String type) {
		System.out.println("Warning: skipping " + type + " without xmi:id");
	}
	
	private static void printNoLabelWarning(final String type, final String id) {
		System.out.print("Warning: " + type + ' ');
		System.out.print(id);
		System.out.println(" does not have a label, ignoring this " + type);
	}
	
	private void readSofa(final Attributes attributes) {
		if (pDocumentText == null)
			pDocumentText = attributes.getValue("sofaString");
	}
	
	private static String noNamespace(final String nodeName) {
		return nodeName.substring(nodeName.indexOf(':') + 1);
	}
	
}
