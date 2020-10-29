/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.xmi2conll;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class Main {
	
	public static void main(final String[] args) {
		if (args.length != 5) {
			printUsage();
			System.exit(1);
			return;
		}
		
		switch (args[0]) {
		case "ca":
			convert(new CorefAnnotatorXmiReader(), args);
			break;
		case "at":
			convert(new AthenXmiReader(), args);
			break;
		default:
			System.out.print("unknown XMI format \"");
			System.out.print(args[0]);
			System.out.println('"');
			printFormats();
			return;
		}
	}
	
	private static void convert(final XmiReader xmiReader, final String[] args) {
		final FileSystem fs = FileSystems.getDefault();
		convert(fs.getPath(args[1]), xmiReader, fs.getPath(args[2]), fs.getPath(args[3]), fs.getPath(args[4]));
	}
	
	private static void convert(final Path xmiFile, final XmiReader xmiReader, final Path tokensFile, final Path outputCoNLL, final Path outputEntities) {
		try {
			try {
				final XMLReader xmlReader;
				try {
					xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
				} catch (final ParserConfigurationException e) {
					exception(e, "XML parser configuration");
					return;
				}
				xmiReader.attachToReader(xmlReader);
				try (final InputStream inputStream = Files.newInputStream(xmiFile)) {
					xmlReader.parse(new InputSource(inputStream));
				}
			} catch (final SAXException e) {
				exception(e, "XML");
				return;
			}
			
			final String documentText = xmiReader.getDocumentText();
			if (documentText == null) {
				System.err.println("No document text found in the XMI file.");
				System.exit(2);
				return;
			}
			
			final ArrayList<Mention> mentions = xmiReader.getMentions();
			
			if (convert(tokensFile, outputCoNLL, basename(outputCoNLL.getFileName().toString()), documentText, mentions)) {
				saveEntities(outputEntities, xmiReader.getEntities(), mentions, documentText);
			} else {
				System.out.print("Document text written to ");
				System.out.println(outputCoNLL.toAbsolutePath().normalize().toString());
				System.exit(2);
			}
		} catch (final IOException e) {
			exception(e, "IO");
		}
	}
	
	private static boolean convert(final Path tokensFile, final Path outputCoNLL, final String documentName, final String documentText, final ArrayList<Mention> mentions) throws IOException {
		final Aligner aligner = new Aligner(documentText);
		final MessageGenerator messageGenerator = new MessageGenerator(documentText);
		final MentionTracker mentionTracker = new MentionTracker(mentions, messageGenerator);
		
		try (final SeekableByteChannel output = Files.newByteChannel(outputCoNLL, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			try (final Writer writer = Channels.newWriter(output, StandardCharsets.UTF_8.newEncoder(), -1)) {
				final boolean ok = convert(documentText, tokensFile, aligner, messageGenerator, mentionTracker, new CoNLL2012Writer(writer, documentName));
				if (!ok) {
					writer.flush(); // only to clear its internal buffer
					output.truncate(0L);
					writer.write(documentText);
				}
				writer.flush();
				return ok;
			}
		}
	}
	
	private static boolean convert(final String documentText, final Path tokensPath, final Aligner aligner, final MessageGenerator messageGenerator, final MentionTracker mentionTracker, final CoNLL2012Writer writer) throws IOException {
		boolean insideSentence = false;
		writer.startDocument();
		try (final BufferedReader reader = Files.newBufferedReader(tokensPath, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				
				final String tokenText = line.trim();
				if ("".equals(tokenText)) {
					if (insideSentence) {
						mentionTracker.endSentence(writer);
						insideSentence = false;
					}
					writer.insertSentenceBoundary();
					continue;
				}
				
				final int tokenStart;
				final int tokenEnd;
				final boolean hasNextToken = aligner.findNextToken();
				if (hasNextToken) {
					tokenStart = aligner.getTokenStart();
					if (documentText.startsWith(tokenText, tokenStart)) {
						final int tokenLength = tokenText.length();
						aligner.passToken(tokenLength);
						tokenEnd = tokenStart + tokenLength;
					} else {
						messageGenerator.printWrongTokenErrorMessage(tokenStart, tokenText);
						return false;
					}
				} else {
					messageGenerator.printNoTokenErrorMessage(aligner.getTokenStart(), tokenText);
					return false;
				}
				
				if (insideSentence) {
					mentionTracker.determinePreviousTokenMentions(tokenStart, tokenEnd, writer);
				} else {
					// first token in sentence
					mentionTracker.startSentence(tokenStart, tokenEnd);
					insideSentence = true;
				}
				
				writer.appendTokenText(tokenText);
			}
			if (insideSentence)
				mentionTracker.endSentence(writer);
		}
		writer.endDocument();
		
		// test whether there are remaining non-whitespace regions in the document text
		final boolean hasNextToken = aligner.findNextToken();
		if (hasNextToken)
			messageGenerator.printRemainingDocumentTextWarning(aligner.getTokenStart());
		
		return true;
	}
	
	private static String basename(final String path) {
		final int dotIndex = path.lastIndexOf('.');
		if (dotIndex < 0)
			return path;
		return path.substring(0, dotIndex);
	}

	private static void saveEntities(final Path outputEntities, final ArrayList<Entity> entities, final Iterable<? extends Mention> mentions, final String documentText) throws IOException {
		sortEntitiesById(entities);
		final HashMap<String, HashMap<String, Long>> references = collectReferences(mentions, documentText);
		
		try (final BufferedWriter writer = Files.newBufferedWriter(outputEntities, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			outputEntities(entities, references, writer);
			writer.flush();
		}
	}
	
	private static void outputEntities(final ArrayList<Entity> entities, final HashMap<String, HashMap<String, Long>> references, final Appendable appendable) throws IOException {
		for (final Entity entity : entities) {
			final String id = entity.getId();
			final HashMap<String, Long> mentionTexts = references.getOrDefault(id, null);
			if ((mentionTexts == null) || mentionTexts.isEmpty())
				continue;
			outputEntityInformation(entity, id, appendable);
			appendable.append('\n');
			outputMentionTexts(mentionTexts, appendable);
		}
	}
	
	private static void outputEntityInformation(final Entity entity, final String id, final Appendable appendable) throws IOException {
		appendable.append(id);
		appendable.append('\t');
		appendable.append(entity.getLabel());
		outputSubEntities(entity.getSubEntityIds(), appendable);
	}
	
	private static void outputSubEntities(final HashSet<String> subEntityIds, final Appendable appendable) throws IOException {
		if (subEntityIds == null)
			return;
		appendable.append('\t');
		final ArrayList<String> subEntityIdsSorted = new ArrayList<>(subEntityIds);
		Collections.sort(subEntityIdsSorted);
		boolean first = true;
		for (final String subEntityId : subEntityIdsSorted) {
			if (first)
				first = false;
			else
				appendable.append(' ');
			appendable.append(subEntityId.toString());
		}
	}
	
	private static void outputMentionTexts(final HashMap<String, Long> mentions, final Appendable appendable) throws IOException {
		for (final Entry<String, Long> reference : mentions.entrySet()) {
			appendable.append('\t');
			appendable.append(reference.getKey());
			appendable.append('\t');
			appendable.append(reference.getValue().toString());
			appendable.append('\n');
		}
	}
	
	private static void sortEntitiesById(final ArrayList<Entity> entities) {
		Collections.sort(entities, (x, y) -> x.getId().compareTo(y.getId()));
	}
	
	private static HashMap<String, HashMap<String, Long>> collectReferences(final Iterable<? extends Mention> mentions, final String documentText) {
		final HashMap<String, HashMap<String, Long>> references = new HashMap<>();
		for (final Mention mention : mentions)
			countMentionString(references, documentText, mention);
		return references;
	}
	
	private static void countMentionString(final HashMap<String, HashMap<String, Long>> references, final String documentText, final Mention mention) {
		CollectionUtil.increment(CollectionUtil.getSubmap(references, mention.getEntityId()), documentText.substring(mention.getBegin(), mention.getEnd()));
	}
	
	private static void exception(final Exception e, final String errorType) {
		System.err.print(errorType);
		System.err.print(" error (");
		System.err.print(e.getClass().getTypeName());
		System.err.print("): ");
		System.err.println(e.getLocalizedMessage());
		System.exit(3);
	}
	
	private static void printUsage() {
		System.err.println("expecting five arguments:");
		System.err.println("XMI format");
		System.err.println("XMI input file");
		System.err.println("tokenization input file");
		System.err.println("CoNLL-2012 output file");
		System.err.println("entities output file");
		printFormats();
	}
	
	private static void printFormats() {
		System.err.println();
		System.err.println("supported XMI formats:");
		System.err.println("  ca  CorefAnnotator (Nils Reiter)");
		System.err.println("  at  Athen (W\u00FCrzburg/Kallimachos)");
	}

}
