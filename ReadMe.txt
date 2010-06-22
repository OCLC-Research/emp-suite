
Extracting Metadata for Preservation - Software Suite

======== Contents ========

emp-suite/
--------/gold-forge
        --------/ Description
                --------/ A web-app for tagging text in the CONLL bracketed format. 
        --------/ Requirements
                --------/ Perl 5.10
                --------/ Plackup http://search.cpan.org/dist/Plack/
                --------/ Web browser (Opera is best)
        --------/ Configuration
                --------/ Open app.yml, change the directory option under setup.
                --------/ Point it to a directory on your system that contains files you want to tag.
        --------/ Usage
                --------/ To start the web-app, run 'plackup' in this directory
                --------/ By default, it starts a server on port 5000, so point your browser to http://localhost:5000/
                --------/ Select a file from the list displayed.
                --------/ Select text in the file you want to tag. 
                        --------/ In Opera, you can type 'p' to tag the selected text as PER, 'm' for MISC, 'o' for ORG, and 'l' for LOC
                                --------/ Typing 'c' will clear the tagging for selected text. It's best to select text at word boundaries,
                                --------/ and to select a few words on either side when clearing a tag.
                        --------/ In other browsers (and Opera) click on the tag type in the legend to change the tag.
                        --------/ You can delete selected text from the page by clicking on 'Delete' (pressing 'd' in Opera)
                                --------/ This can be useful when another tagging tool adds space characters.
                --------/ Clicking on 'As CONLL' will show the text as it looks in CONLL.
                --------/ Clicking on 'As HTML' will show the color-ized version of the tagging.
                --------/ To save the CONLL tagged version, you must click on 'As CONLL' and then manually opy and paste the text into a file.

--------/uiuc-ner
        --------/ Description
                --------/ Named Entity Recognizing library developed by UIUC's Cognitive Computation Group.
        --------/ Requirements
                --------/ Java 1.6 - http://java.sun.com/
                --------/ Patch 2.6.1 - http://www.gnu.org/software/patch/patch.html
                --------/ Bash
        --------/ Compilation
                --------/ Download the UIUC CCG Named Entity Tagger from http://l2r.cs.uiuc.edu/~cogcomp/asoftware.php?skey=FLBJNE
                        --------/ ( As of 2010-06-18, the downloaded file is named LBJNERTagger1.2.zip, but actually contains version 1.11 )
                --------/ Move the downloaded zip file into the uiuc-ner directory.
                --------/ Execute the 'make-install' script.
                        --------/ This script will unzip the file, patch the source, then build and copy JAR files into the emp-suite/lib directory.


