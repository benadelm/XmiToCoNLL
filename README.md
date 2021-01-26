# XmiToConll

A tool, developed in the research project [hermA](https://www.herma.uni-hamburg.de/en.html), for creating a CoNLL-2012 file with a specific tokenization and the coreference information stored in an `.xmi` file, such as those saved by  [CorefAnnotator](https://github.com/nilsreiter/CorefAnnotator). It is useful, for example, if a specific tokenization is required or if the tokenization produced by CorefAnnotator’s own CoNLL-2012 exporter is undesirable for some other reason.

This tool supports `.xmi` files as created by CorefAnnotator up to at least version 1.14.3 and should also be able to read `.xmi` files created by [Athen](https://www.informatik.uni-wuerzburg.de/is/open-source-tools/athen/) (not tested with the latest version).

# Installation

The software has been tested with Windows 10 and Linux. Being written in Java, it should run on any platform Java supports; you will need a Java runtime to run the software. It has been developed and tested with Java 8, but newer versions may also work.

# Input

The program expects five positional command-line arguments:

1. coreference format specifier
2. path to the input `.xmi` file
3. path to the tokenization input file
4. path to the output CoNLL file
5. path to a file where to write information about the referenced entities

The *coreference format specifier* can be

* `ca` for `.xmi` files created by CorefAnnotator;
* `at` for `.xmi` files created by Athen.

See *functionality details* below for a description of how `.xmi` files are interpreted by the tool depending on the format.

Paths can be absolute or relative. Output files (fourth and fifth argument) will be created or overwritten.

Dummy example for command line arguments:

	ca coreference-annotations.xmi tokenization.txt output.conll output-entities.txt

The tokenization input file is a UTF-8 plain text file with one token per line, sentences being separated by an empty line, for example:

	This
	is
	a
	sentence
	.
	
	This
	is
	another
	sentence
	.

# Output

## CoNLL File

The CoNLL output file is a UTF-8 plain text file with the following format:

* The first line starts with `#` indicating a comment. This line always has the form `#begin document (<document name>); part 0` where `<document name>` is the file name of the input file without extension.
* Likewise, the last line is `#end document <document name>` (with the same `<document name>`).
* Sentences are separated by empty lines, as provided in the tokenization input.
* The other lines correspond to the tokens from the tokenization input.

Conforming to the CoNLL-2012 format, the token lines consist of the fields described [here (section ‘*_conll File Format’)](http://conll.cemantix.org/2012/data.html), separated by tabulator characters (U+0009). However, only the first four columns (Document ID, Part number, Word number, Word itself) as well as the coreference column (`N`) are filled:
* The Document ID is the same as the `<document name>` above.
* The Part number is always set to `0` (zero).
* The Word number is obtained by numbering the tokens sequentially, starting with `1` for the first token of each sentence.
* The Word itself is the corresponding token from the tokenization input.
 
All other columns are set to `_` (underscore). As there are no Predicate Arguments, the coreference information (‘column `N`’) resides in column twelve.

The coreference column lists entity references, called *mentions*. Only mentions that start and/or end with the corresponding token are listed. If several mentions start or end with the token, they appear in arbitrary order, separated by pipe characters (`|`). The encoding of a single mention consists of, in this order,
* an opening bracket `(` if the mention starts with the token;
* an ID that is the same for all coreferring mentions (that is, the ID indicates the coreference chain);
* a closing bracket `)` if the mention ends with the token.
As only those mentions that start and/or end with the token are listed, there is always either an opening or a closing bracket present, possibly both (if the mention comprises only a single token). As an ID that is the same for all coreferring mentions the tool simply uses the ID of the referenced entity from the `.xmi` file.

Example:

* entity 123 starts at this token (and ends at a later one): `(123`
* entity 123 ends at this token (and started at an earlier one): `123)`
* entity 123 comprises only this token (starts and ends here): `(123)`

If a token is neither the start nor end of a mention, the coreference field is set to `_` (underscore).

## Information about the Entities

The entities information file is a UTF-8 plain text file. For every entity from the `.xmi` file, there is one line with two or three fields separated by tabulator characters (U+0009):

1. the entity ID
2. the entity label
3. only in the case of an entity group: the IDs of the entities grouped together, separated by spaces

This line is followed by zero or more lines starting with a tabulator character (U+0009). These lines list, in arbitrary order, the different text passages marked as mentions of the entity. The tabulator character with which the line starts is followed by:

1. the text passage
2. another tabulator character
3. the (absolute) count how often this text passage appears as a mention of the entity

Example (from <i lang="de">Aus guter Familie</i>):

	31673	Agathe
		das arme Kind	1
		Hochgeährdestes Frölen Heidling	1
		Fräulein	6
		seiner	1
		seinen	1
		die Kranke	1
		Mein gutes Kind - meine gute Kleine	1
		dies Mädchen	1
		Die keusche Agathe	1
		das gute Kind	2
		eine junge Dame	1
		eine Tochter	1
		...
	31728	einer der Studenten des Professors aus Zürich (in der Schweiz)
		einer der Studenten	1
		der Student	2
		seinen	1
		Ich	1
		ich	1
	92118	Herr von Woszenski
		sein	2
		Woszenski	8
		seine	5
		Er	10
		einen polnischen Künstler, Kasimir von Woszenski	1
		...
	92218	Frau von Woszenska
		die	2
		Frau Woszenska	1
		Frau von Woszenska	23
		deren	2
		mein	1
		Frau von Woszenski	4
		...
	92848	Ehepaar von Woszenska	92118 92218
		ihnen	2
		des Ehepaares	1
		wir	1
		ihrem	1
		Deine Woszenskis	1
		Woszenskis	11
		dem Malerehepaar	1
		ihrer	1
		diese Woszenskis	1
		das Ehepaar	1
		Sie	1
		sie alle beide	1

Entity 92848 (<span lang="de">Ehepaar von Woszenska</span>) is a group of the two entities 92118 (<span lang="de">Herr von Woszenski</span>) and 92218 (<span lang="de">Frau von Woszenska</span>).

# Functionality Details

If the coreference format specifier is set to `ca` for CorefAnnotator, the tool expects coreference-annotated text passages to be stored in the `.xmi` file as XML elements whose name is `Mention`, optionally preceded by an XML namespace, and that are immediate children of the root element. The namespace is ignored. This is because the namespace may vary depending on the version of CorefAnnotator (in version 1.14.3 it is `v1`, but used to be `api` in earlier versions). The tool expects begin and end of the annotated text span to be specified as character offsets in the `begin` and `end` attributes, respectively. The ID of the entity which the text passage is annotated to be a reference to is expected to be given by the `Entity` attribute. (The value of this attribute is the ID that will be used in the coreference column of the CoNLL output.) If any of these three attributes is missing, or if the values of `begin` or `end` is not a whole number, the tool gives a warning and ignores the element. Other attributes are ignored.

Information about the entities referenced by the `Entity` attributes of `Mention` elements is expected to be stored as XML elements whose name is `Entity` or `EntityGroup`, in either case optionally preceded by an XML namespace, and that are, again, immediate children of the root element. These elements are expected to have an `xmi:id` attribute providing the ID used in `Entity` attributes of `Mention` elements to reference the entity. (However, the tool does not check whether all `Entity` attributes give the ID of an existing `Entity` or `EntityGroup` element.) Additionally, the elements are expected to have a `Label` attribute containing a textual label for the entity or entity group. (This is used for the entity information output file explained above.) Furthermore, `EntityGroup` elements are expected to have a `Members` attribute listing, separated by spaces, the IDs of the entities comprised by the group. If `xmi:id` or `Label` are missing, the tool outputs a warning and ignores the element; if an EntityGroup lacks a `Members` attribute, a warning is issued and the element is treated like a plain `Entity` (this only has implications for the entity information output file). Other attributes are ignored.

If the coreference format specifier is set to `at` for Athen, the tool expects coreference-annotated text passages to be stored in the `.xmi` file as XML elements that are immediate children of the root element and whose name (including namespace) is `type:NamedEntity`. Again, begin and end of the annotated text span are expected to be specified as character offsets in the `begin` and `end` attributes, respectively. The ID of the entity which the text passage is annotated to be a reference to is expected to be given by the `ID` attribute. (Like above, the value of this attribute will be used in the coreference column of the CoNLL output.) If any of these three attributes is missing, or if the values of `begin` or `end` is not a whole number, the tool gives a warning and ignores the element. Other attributes are ignored.

Unlike CorefAnnotator, Athen does not provide elements with information about the annotated entities, but `type:NamedEntity` elements may have additional attributes with such information. This tool only takes `Name` attributes into account, which play a similar role as the `Label` attributes of CorefAnnotator’s `Entity` or `EntityGroup` elements, but are attached to individual mentions and may differ among different mentions of the same entity. This tool collects these names and takes the most frequent one per entity ID as the label of the entity (that is printed to the entity information output file).

To determine which tokens belong to which mention, the tool tries to align the provided tokenization with the document text stored in the `.xmi` file, as the character indices from the `begin` and `end` attributes refer to that document text. The document text is expected to be the value of the `sofaString` attribute of an XML element that is an immediate child of the root element and whose name is `cas:Sofa`. (If the coreference format specifier is set to `ca` for CorefAnnotator, the namespace is ignored, so `Sofa` with any preceding namespace would be accepted.) There should only be one `cas:Sofa` per `.xmi` file and the first encountered `cas:Sofa` with a `sofaString` attribute is used. (Other attributes are always ignored.)

Any XML elements that do not fulfill the above criteria are ignored by the tool.

The tool expects the document text (more precisely, a prefix of it) stored in the `.xmi` file to consist exactly of the tokens from the provided tokenization, in order, with whitespace between them. ‘Whitespace’ is zero or more *whitespace characters*, where the following Unicode code points are considered whitespace characters:
* U+0009 CHARACTER TABULATION
* U+000A LINE FEED
* U+000B LINE TABULATION
* U+000C FORM FEED
* U+000D CARRIAGE RETURN
* U+001C INFORMATION SEPARATOR FOUR
* U+001D INFORMATION SEPARATOR THREE
* U+001E INFORMATION SEPARATOR TWO
* U+001F INFORMATION SEPARATOR ONE
* any code point having Unicode category `Z` (Separator), which includes ordinary spaces (U+0020 SPACE).

If this is not the case, the first token that does not match and the corresponding location in the document text are reported, including that case that the document text ends before all tokens from the provided tokenization have been encountered. Furthermore, the document text is written to the CoNLL output file (instead of the CoNLL-2012 output) so that it can be used to create a fitting tokenization, and the tool exits with exit code 2.

If there is still document text left after all tokens from the tokenization have been encountered, a warning is issued, but this situation is *not* considered an error and normal CoNLL, not the document text, is written to the output file. (This means that you cannot use this tool to extract the document text by simply passing an empty tokenization file, as the output would be an empty CoNLL file.)

A mention is considered to belong to a token from the tokenization if its span (from `begin` to `end`, see above) overlaps with the text span of the token, as determined by the above-mentioned alignment. That is, the `begin` of the mention has to be smaller than the end of the token and the `end` of the mention has to be greater than the start of the token. For instance, given the document text

	This is a documenttext.

and the tokenization

	This
	is
	a
	document
	text
	.

a mention with `begin="15"` and `end="20"` would be assigned to the tokens `document` and `text`, while a mention with `begin="4"` and `end="8"` would be assigned to only the token `is` and a mention with `begin="18"` and `end="22"` would be assigned to only the token `text`.

A warning is shown if a mention does not belong to any token according to the above rule. Such a mention would cover only whitespace, which is likely an annotation error.

The tool ensures that every mention opened in a sentence is closed in the same sentence in the CoNLL output. If a mention crosses a sentence boundary, that is, if it is assigned to tokens from different sentences (separated by an empty line in the tokenization), the mention is closed upon the last token of the sentence and re-opened upon the first token of the next sentence. If the mention crosses more than one sentence boundary, this is repeated for every crossed sentence boundary. For instance, given the document text

	Sentence one. Sentence two! Sentence three?

and the tokenization

	Sentence
	one
	.
	
	Sentence
	two
	!
	
	Sentence
	three
	?

a mention of entity `123` with `begin="9"` and `end="30"` would be represented as follows in the output (showing only word and coreference column here):

	Sentence	_
	one	(123
	.	123)
	
	Sentence	(123
	two	_
	!	123)
	
	Sentence	(123)
	three	_
	?	_

Normally a mention should not cross sentence boundaries (one possible reason are sentence tokenization errors), so if this happens, a warning is issued, too.

Successful termination of the tool is indicated by exit code 0, failure to parse the command line by exit code 1, alignment errors (with document text output) by exit code 2 and other errors (I/O errors, XML parsing errors etc.) by exit code 3 or other exit codes specific to the exception handling of the Java Virtual Machine.