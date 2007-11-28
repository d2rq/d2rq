var snorql = new Snorql();

function Snorql() {
    this._browserBase = null;
    this._endpoint = null;
    this._namespaces = {};
    this._xsltDOM = null;

    this.start = function() {
        // TODO: Extract a QueryType class
        this.setBrowserBase(document.location.href.replace(/\?.*/, ''));
        var endpoint = document.location.href.match(/^([^?]*)snorql\//)[1] + 'sparql';
        this.setEndpointURL(endpoint);
        this.setNamespaces(D2R_namespacePrefixes);
        this.updateOutputMode();
        var match = document.location.href.match(/\?(.*)/);
        var queryString = match ? match[1] : '';
        if (!queryString) {
            document.getElementById('querytext').value = 'SELECT * WHERE {\n...\n}';
            return;
        }
        var querytext = null;
        if (queryString == 'classes') {
            var resultTitle = 'List of all classes:';
            var query = 'SELECT DISTINCT ?class\n' +
                    'WHERE { [] a ?class }\n' +
                    'ORDER BY ?class';
        }
        if (queryString == 'properties') {
            var resultTitle = 'List of all properties:';
            var query = 'SELECT DISTINCT ?property\n' +
                    'WHERE { [] ?property [] }\n' +
                    'ORDER BY ?property';
        }
        var match = queryString.match(/property=([^&]*)/);
        if (match) {
            var resultTitle = 'All uses of property ' + decodeURIComponent(match[1]) + ':';
            var query = 'SELECT ?resource ?value\n' +
                    'WHERE { ?resource <' + decodeURIComponent(match[1]) + '> ?value }\n' +
                    'ORDER BY ?resource ?value';
        }
        var match = queryString.match(/class=([^&]*)/);
        if (match) {
            var resultTitle = 'All instances of class ' + decodeURIComponent(match[1]) + ':';
            var query = 'SELECT ?instance\n' +
                    'WHERE { ?instance a <' + decodeURIComponent(match[1]) + '> }\n' +
                    'ORDER BY ?instance';
        }
        var match = queryString.match(/describe=([^&]*)/);
        if (match) {
            var resultTitle = 'Description of ' + decodeURIComponent(match[1]) + ':';
            var query = 'SELECT ?property ?hasValue ?isValueOf\n' +
                    'WHERE {\n' +
                    '  { <' + decodeURIComponent(match[1]) + '> ?property ?hasValue }\n' +
                    '  UNION\n' +
                    '  { ?isValueOf ?property <' + decodeURIComponent(match[1]) + '> }\n' +
                    '}\n' +
                    'ORDER BY (!BOUND(?hasValue)) ?property ?hasValue ?isValueOf';
        }
        if (queryString.match(/query=/)) {
            var resultTitle = 'SPARQL results:';
            querytext = this._betterUnescape(queryString.match(/query=([^&]*)/)[1]);
            var query = prefixes + querytext;
        }
        if (!querytext) {
            querytext = query;
        }
        document.getElementById('querytext').value = querytext;
        this.displayBusyMessage();
        var service = new SPARQL.Service(this._endpoint);
        var dummy = this;
        service.query(query, {
            success: function(json) {
                dummy.displayResult(json, resultTitle);
            },
            failure: function(report) {
                var message = report.responseText.match(/<pre>([\s\S]*)<\/pre>/);
                if (message) {
                    dummy.displayErrorMessage(message[1]);
                } else {
                    dummy.displayErrorMessage(report.responseText);
                }
            }
        });
    }

    this.setBrowserBase = function(url) {
        this._browserBase = url;
    }

    this.setEndpointURL = function(url) {
        this._endpoint = url;
        var newTitle = 'Snorql: Exploring ' + url;
        this._display(document.createTextNode(newTitle), 'title');
        document.title = newTitle;
    }

    this.setNamespaces = function(namespaces) {
        this._namespaces = namespaces;
        this._display(document.createTextNode(this._getPrefixes()), 'prefixestext');
    }

    this.updateOutputMode = function() {
        if (this._xsltDOM == null) {
            this._xsltDOM = document.getElementById('xsltinput');
        }
        var el = document.getElementById('xsltcontainer');
        while (el.childNodes.length > 0) {
            el.removeChild(el.firstChild);
        }
        if (this._selectedOutputMode() == 'xslt') {
            el.appendChild(this._xsltDOM);
        }
    }

    this.resetQuery = function() {
        document.location = this._browserBase;
    }

    this.submitQuery = function() {
        var mode = this._selectedOutputMode();
        if (mode == 'browse') {
            document.getElementById('queryform').action = this._browserBase;
            document.getElementById('query').value = document.getElementById('querytext').value;
        } else {
            document.getElementById('query').value = this._getPrefixes() + document.getElementById('querytext').value;
            document.getElementById('queryform').action = this._endpoint;
        }
        document.getElementById('jsonoutput').disabled = (mode != 'json');
        document.getElementById('stylesheet').disabled = (mode != 'xslt' || !document.getElementById('xsltstylesheet').value);
        if (mode == 'xslt') {
            document.getElementById('stylesheet').value = document.getElementById('xsltstylesheet').value;
        }
        document.getElementById('queryform').submit();
    }

    this.displayBusyMessage = function() {
        var busy = document.createElement('div');
        busy.className = 'busy';
        busy.appendChild(document.createTextNode('Executing query ...'));
        this._display(busy, 'result');
    }

    this.displayErrorMessage = function(message) {
        var pre = document.createElement('pre');
        pre.innerHTML = message;
        this._display(pre, 'result');
    }

    this.displayResult = function(json, resultTitle) {
        var div = document.createElement('div');
        var title = document.createElement('h2');
        title.appendChild(document.createTextNode(resultTitle));
        div.appendChild(title);
        if (json.results.bindings.length == 0) {
            var p = document.createElement('p');
            p.className = 'empty';
            p.appendChild(document.createTextNode('[no results]'));
            div.appendChild(p);
        } else {
            div.appendChild(new SPARQLResultFormatter(json, this._namespaces).toDOM());
        }
        this._display(div, 'result');
    }

    this._display = function(node, whereID) {
        var where = document.getElementById(whereID);
        if (!where) {
            alert('ID not found: ' + whereID);
            return;
        }
        while (where.firstChild) {
            where.removeChild(where.firstChild);
        }
        if (node == null) return;
        where.appendChild(node);
    }

    this._selectedOutputMode = function() {
        return document.getElementById('selectoutput').value;
    }

    this._getPrefixes = function() {
        prefixes = '';
        for (prefix in this._namespaces) {
            var uri = this._namespaces[prefix];
            prefixes = prefixes + 'PREFIX ' + prefix + ': <' + uri + '>\n';
        }
        return prefixes;
    }

    this._betterUnescape = function(s) {
        return unescape(s.replace(/\+/g, ' '));
    }
}

/*
===========================================================================
SPARQLResultFormatter: Renders a SPARQL/JSON result set into an HTML table.

var namespaces = { 'xsd': '', 'foaf': 'http://xmlns.com/foaf/0.1' };
var formatter = new SPARQLResultFormatter(json, namespaces);
var tableObject = formatter.toDOM();
*/
function SPARQLResultFormatter(json, namespaces) {
    this._json = json;
    this._variables = this._json.head.vars;
    this._results = this._json.results.bindings;
    this._namespaces = namespaces;

    this.toDOM = function() {
        var table = document.createElement('table');
        table.className = 'queryresults';
        table.appendChild(this._createTableHeader());
        for (var i in this._results) {
            table.appendChild(this._createTableRow(this._results[i], i));
        }
        return table;
    }

    // TODO: Refactor; non-standard link makers should be passed into the class by the caller
    this._getLinkMaker = function(varName) {
        if (varName == 'property') {
            return function(uri) { return '?property=' + encodeURIComponent(uri); };
        } else if (varName == 'class') {
            return function(uri) { return '?class=' + encodeURIComponent(uri); };
        } else {
            return function(uri) { return '?describe=' + encodeURIComponent(uri); };
        }
    }

    this._createTableHeader = function() {
        var tr = document.createElement('tr');
        for (var i in this._variables) {
            var th = document.createElement('th');
            th.appendChild(document.createTextNode(this._variables[i]));
            tr.appendChild(th);
        }
        return tr;
    }

    this._createTableRow = function(binding, rowNumber) {
        var tr = document.createElement('tr');
        if (rowNumber % 2) {
            tr.className = 'odd';
        } else {
            tr.className = 'even';
        }
        for (var v in this._variables) {
            var varName = this._variables[v];
            td = document.createElement('td');
            td.appendChild(this._formatNode(binding[varName], varName));
            tr.appendChild(td);
        }
        return tr;
    }

    this._formatNode = function(node, varName) {
        if (!node) {
            return this._formatUnbound(node, varName);
        }
        if (node.type == 'uri') {
            return this._formatURI(node, varName);
        }
        if (node.type == 'bnode') {
            return this._formatBlankNode(node, varName);
        }
        if (node.type == 'literal') {
            return this._formatPlainLiteral(node, varName);
        }
        if (node.type == 'typed-literal') {
            return this._formatTypedLiteral(node, varName);
        }
        return document.createTextNode('???');
    }

    this._formatURI = function(node, varName) {
        var span = document.createElement('span');
        span.className = 'uri';
        var a = document.createElement('a');
        a.href = this._getLinkMaker(varName)(node.value);
        a.title = '<' + node.value + '>';
        var qname = this._toQName(node.value);
        if (qname) {
            a.appendChild(document.createTextNode(qname));
            span.appendChild(a);
        } else {
            a.appendChild(document.createTextNode(node.value));
            span.appendChild(document.createTextNode('<'));
            span.appendChild(a);
            span.appendChild(document.createTextNode('>'));
        }
        match = node.value.match(/^(https?|ftp|mailto|irc|gopher|news):/);
        if (match) {
            span.appendChild(document.createTextNode(' '));
            var externalLink = document.createElement('a');
            externalLink.href = node.value;
            img = document.createElement('img');
            img.src = 'link.png';
            img.alt = '[' + match[1] + ']';
            img.title = 'Go to Web page';
            externalLink.appendChild(img);
            span.appendChild(externalLink);
        }
        return span;
    }

    this._formatPlainLiteral = function(node, varName) {
        var text = '"' + node.value + '"';
        if (node['xml:lang']) {
            text += '@' + node['xml:lang'];
        }
        return document.createTextNode(text);
    }

    this._formatTypedLiteral = function(node, varName) {
        var text = '"' + node.value + '"';
        if (node.datatype) {
            text += '^^' + this._toQNameOrURI(node.datatype);
        }
        if (this._isNumericXSDType(node.datatype)) {
            var span = document.createElement('span');
            span.title = text;
            span.appendChild(document.createTextNode(node.value));
            return span;
        }
        return document.createTextNode(text);
    }

    this._formatBlankNode = function(node, varName) {
        return document.createTextNode('_:' + node.value);
    }

    this._formatUnbound = function(node, varName) {
        var span = document.createElement('span');
        span.className = 'unbound';
        span.title = 'Unbound'
        span.appendChild(document.createTextNode('-'));
        return span;
    }

    this._toQName = function(uri) {
        for (prefix in this._namespaces) {
            var nsURI = this._namespaces[prefix];
            if (uri.indexOf(nsURI) == 0) {
                return prefix + ':' + uri.substring(nsURI.length);
            }
        }
        return null;
    }

    this._toQNameOrURI = function(uri) {
        var qName = this._toQName(uri);
        return (qName == null) ? '<' + uri + '>' : qName;
    }

    this._isNumericXSDType = function(datatypeURI) {
        for (i in this._numericXSDTypes) {
            if (datatypeURI == this._xsdNamespace + this._numericXSDTypes[i]) {
                return true;
            }
        }
        return false;
    }
    this._xsdNamespace = 'http://www.w3.org/2001/XMLSchema#';
    this._numericXSDTypes = ['long', 'decimal', 'float', 'double', 'int',
        'short', 'byte', 'integer', 'nonPositiveInteger', 'negativeInteger',
        'nonNegativeInteger', 'positiveInteger', 'unsignedLong',
        'unsignedInt', 'unsignedShort', 'unsignedByte'];
}
