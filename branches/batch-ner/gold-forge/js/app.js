

// Default view is HTML
var currentView = 'html';

// return the selection object
function my_captureSelection() {
	var userSelection;
	if (window.getSelection) {
		userSelection = window.getSelection();
	}
	else if (document.selection) { // should come last; Opera!
		userSelection = document.selection.createRange();
	}
	return userSelection;
}

// return a range object for a given selection
function getRangeObject(selectionObject) {
	if (selectionObject.getRangeAt) {
		return selectionObject.getRangeAt(0);
	}
	else { // Safari!
		var range = document.createRange();
		range.setStart(selectionObject.anchorNode,selectionObject.anchorOffset);
		range.setEnd(selectionObject.focusNode,selectionObject.focusOffset);
		return range;
	}
}

// tag the selection with the supplied CONLL tag type
function conll_tag(type) {
	var s = my_captureSelection();
	var r = getRangeObject(s);
	s.removeAllRanges();

	var replacementText = new Array();

	var elementInRange = false;

	var df = r.cloneContents();
	for (var i = 0; i < df.childNodes.length; i++) {
		var child = df.childNodes[i];
		if (child.nodeType == 1) {
			replacementText.push(child.innerHTML);
			elementInRange = true;
		}
		else if (child.nodeType == 3) {
			replacementText.push(child.data);
		}
	}

	var text = document.createTextNode(replacementText.join(''));
	var replacementNode = text;
	if ( "CLEAR" != type ) {
		replacementNode = document.createElement("span");
		replacementNode.className = type;
		replacementNode.innerHTML = text.nodeValue;
	}
	r.deleteContents();
	r.insertNode(replacementNode);
}

// Escape text when converting to CONLL
function escape_to_conll (str) {
	return str.replace('[', '&#x5b;').replace(']', '&#x5d;');
}

// Convert the text to CONLL markup
function switch_toCONLL(){
	if ( currentView == 'conll' ) { return; }
	var elem = document.getElementById('htmlview');

	var target = document.getElementById('conllview');
	if ( null == target ) {
		target = document.createElement('div');
		target.className = 'conllview';
		target.id = 'conllview';
	}
	// Empty the target
	target.innerHTML = '';

	for (var i = 0; i < elem.childNodes.length; i++) {
		var child = elem.childNodes[i];
		var new_child;
		if (child.nodeType == 1) { // ELEMENT NODE
			var conll = child.getAttribute("class");
			var new_child = document.createTextNode('[');
			new_child.appendData(conll);
			new_child.appendData(' ');
			new_child.appendData(escape_to_conll(child.innerHTML));
			new_child.appendData(']');
		}
		else if (child.nodeType == 3) { // TEXT NODE
			new_child = child.cloneNode(true);
		}
		target.appendChild(new_child);
	}
	elem.style.display = 'none';
	target.style.display = 'block';
	currentView = 'conll';
}


// Escape text when converting to HTML
function escape_to_html(str) {
	return str.replace('&#x5b;', '[').replace('&#x5d;', ']').replace('&amp;', '&');
}

// Convert the text to HTML markup (tags demarcated with <span>s)
function switch_toHTML(){
	if ( currentView == 'html' ) { return; }
	var view = document.getElementById('conllview');

	var target = document.getElementById('htmlview');
	if ( null == target ) {
		target = document.createElement('div');
		target.className = 'htmlview';
		target.id = 'htmlview';
	}
	// Empty the target
	target.innerHTML = '';

	for (var i = 0; i < view.childNodes.length; i++) {
		var child = view.childNodes[i];
		if (child.nodeType == 3) { // TEXT NODE
			var data = child.data;
			var classname;
			if ( '[LOC ' == data.substr(0, 5) ) {
				classname = "LOC";
				data = data.substr(5, data.length - 6);
			}
			else if ( '[ORG ' == data.substr(0, 5) ) {
				classname = "ORG";
				data = data.substr(5, data.length - 6);
			}
			else if ( '[PER ' == data.substr(0, 5) ) {
				classname = "PER";
				data = data.substr(5, data.length - 6);
			}
			else if ( '[MISC ' == data.substr(0, 6) ) {
				classname = "MISC";
				data = data.substr(6, data.length - 7);
			}
			else {
				target.appendChild(child.cloneNode(true));
				continue;
			}

			var new_child = document.createElement('span');
			new_child.className = classname;
			new_child.appendChild(document.createTextNode(escape_to_html(data)));
			target.appendChild(new_child);
		}
		else {
			alert(child.nodeType);
		}

	}

	view.style.display = 'none';
	target.style.display = 'block';
	currentView = 'html';
}

// tag the selection CONL-wise depending on key pressed
function key_dispatch (evt) {
	var key = evt.charCode || evt.keyCode || evt.which;
	if ( 99 == key ) { // "c"
		conll_tag("CLEAR");
	}
	else if ( 109 == key ) { // "m"
		conll_tag("MISC");
	}
	else if ( 108 == key ) { // "l"
		conll_tag("LOC");
	}
	else if ( 111 == key ) { // "o"
		conll_tag("ORG");
	}
	else if ( 112 == key ) { // "p"
		conll_tag("PER");
	}
	else if ( 100 == key ) { // "d"
		delete_selection();
	}
} 


function delete_selection() {
	var s = my_captureSelection();
	var r = getRangeObject(s);
	s.removeAllRanges();
	r.deleteContents();
}

