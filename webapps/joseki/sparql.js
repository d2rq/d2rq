/**********************************************************
  Copyright (c) 2006, 
    Lee Feigenbaum ( lee AT thefigtrees DOT net )
    Elias Torres   ( elias AT torrez DOT us )
  All rights reserved.

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
**********************************************************/

var SPARQL  = {}; // SPARQL namespace


/**
 * Both SPARQL service objects and SPARQL query objects receive one query utility method
 * per entry in this dictionary. The key is the name of the method, and the value is a function
 * that transforms the standard JSON output into a more useful form. The return value of a
 * transformation function is passed into any 'success' callback function provided when the query
 * is issued. The following transformations are included:
 *   + query -- identity transform; returns the JSON structure unchanged
 *   + ask -- for ASK queries; returns a boolean value indicating the answer to the query
 *   + selectValues -- for SELECT queries with a single variable; returns an array containing
 *       the answers to the query
 *   + selectSingleValue -- for SELECT queries returning one column with one row; returns the
 *       value in the first (and presumably, only) cell in the resultset
 *   + selectValueArrays -- for SELECT queries returning independent columns; returns a hash
 *       keyed on variable name with values as arrays of answers for that variable. Useful
 *       for UNION queries.
 *   Note that all of the transformations that return values directly lose any type information
 *   and the ability to distinguish between URIs, blank nodes, and literals.
 */
SPARQL._query_transformations = {
    query: function (o) { return o; },
    ask: function (o) { return o['boolean']; },
    selectValues: function (o) {
        var v = o.head.vars[0]; // assume one variable
        var values = [];
        for (var i = 0; i < o.results.bindings.length; i++)
            values.push(o.results.bindings[i][v].value);
        return values;
    },
    selectSingleValue: function(o) { return o.results.bindings[0][o.head.vars[0]].value; },
    selectValueArrays: function(o) {
        // factor by value (useful for UNION queries)
        var ret = {};
        for (var i = 0; i < o.head.vars.length; i++)
            ret[o.head.vars[i]] = [];
        for (var i = 0; i < o.results.bindings.length; i++)
            for (var v in o.results.bindings[i])
                if (ret[v] instanceof Array) ret[v].push(o.results.bindings[i][v].value);
        return ret;
    }
};

// A SPARQL service represents a single endpoint which implements the HTTP (GET or POST) 
// bindings of the SPARQL Protocol. It provides convenience methods to set dataset and
// prefix options for all queries created for this endpoint.
SPARQL.Service = function(endpoint) {
    //---------------
    // private fields
    var _endpoint = endpoint;
    var _default_graphs = [];
    var _named_graphs = [];
    var _prefix_map = {};
    var _method = 'POST';
    var _output = 'json';

    //----------
    // accessors
    this.endpoint = function() { return _endpoint; };
    this.defaultGraphs = function() { return _default_graphs; };
    this.namedGraphs = function() { return _named_graphs; };
    this.prefixes = function() { return _prefix_map; };
    this.method = function() { return _method; };
    this.output = function() { return _output; };
    
    //---------
    // mutators
    function _add_graphs(toAdd, arr) {
        if (toAdd instanceof Array)
            for (var i = 0; i < toAdd.length; i++) arr.push(toAdd[i]);
        else
            arr.push(toAdd);
    }
    this.addDefaultGraph = function(g) { _add_graphs(g, this.defaultGraphs()); };
    this.addNamedGraph = function(g) { _add_graphs(g, this.namedGraphs()); };
    this.setPrefix = function(p, u) { this.prefixes()[p] = u; };
    this.createQuery = function() { return new SPARQL.Query(this); };
    this.setMethod = function(m) {
        if (m != 'GET' && m != 'POST') throw("HTTP methods other than GET and POST are not supported.");
        _method = m;
    };
    this.setOutput = function(o) { _output = o; }

    //---------------
    // public methods

    // use our varied transformations to create the various shortcut methods of actually 
    // issuing queries without explicitly creating a query object
    for (var query_form in SPARQL._query_transformations) {
        // need the extra function to properly scope query_form (qf)
        this[query_form] = (function(qf) {
            return function(queryString, callback) {
                var q = this.createQuery();
                q._doQuery(queryString, callback, SPARQL._query_transformations[qf]);
            };
        })(query_form);
    }
    
    //------------
    // constructor
    
    if (!_endpoint)
        return null;
    
    return this;
}

/**
 * A SPARQL query object should be created using the createQuery method of a SPARQL
 * service object. It allows prefixes and datasets to be defined specifically for
 * a single query, and provides introspective methods to see the query string and the
 * full (HTTP GET) URL of the query.
 */
SPARQL.Query = function(service) {
    //---------------
    // private fields
    var _conn = null;
    var _service = service;
    var _default_graphs = clone_obj(service.defaultGraphs()); // prevent future updates from affecting us
    var _named_graphs = clone_obj(service.namedGraphs());
    var _prefix_map = clone_obj(service.prefixes());
    var _user_query = ''; // doesn't include auto-generated prefix declarations
    var _method = service.method();
    var _output = service.output();

    //------------------
    // private functions
    function _create_json(text) { 
        if (!text)
            throw("SPARQL service returned empty successful response.");
        // make sure this is safe JSON
        // see: http://www.ietf.org/internet-drafts/draft-crockford-jsonorg-json-03.txt
        
        // (1) strip out quoted strings
        var no_strings = text.replace(/"(\\.|[^"\\])*"/g, '');
        // (2) make sure that all the characters are explicitly part of the JSON grammar
        // (in particular, note as discussed in the IETF submission, there are no assignments
        //  or function invocations allowed by this reg. exp.)
        var hasBadCharacter = /[^,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]/.test(no_strings);
        // (3) evaluate the JSON string, returning its contents
        if (!hasBadCharacter)
            return eval('(' + text + ')');
        return null; 
    }   
    
    function clone_obj(o) { 
        var o2 = o instanceof Array ? [] : {}; 
        for(var x in o) {o2[x] = o[x];} 
        return o2;
    }

    //----------------
    // private methods
    this._doCallback = function(cb, which, arg) {
        var user_data = "argument" in cb ? cb.argument : null;
        if (which in cb) {
            if (cb.scope)
                cb[which].apply(scope, [arg, user_data]);
            else
                cb[which](arg, user_data);
        }
    }
    
    this._queryFailure = function(resp) {
        this._doCallback(resp.argument.callback, 'failure', resp /* just pass through the connection response object */);
    };
    this._querySuccess = function(resp) {
        this._doCallback(resp.argument.callback, 'success', resp.argument.transformer(
            _output == 'json' ? _create_json(resp.responseText) : resp.responseText
        ));
    };
    this._doQuery = function(queryString, callback, transformer) {
        _user_query = queryString;
        if (_method == 'GET') {
            _conn = YAHOO.util.Connect.asyncRequest('GET', this.queryUrl(), 
                {
                    scope:this, 
                    success:this._querySuccess, 
                    failure:this._queryFailure, 
                    argument: {transformer:transformer, callback:callback}
                },
                null);
        } else if (_method == 'POST') {
            _conn = YAHOO.util.Connect.asyncRequest('POST', this.service().endpoint(), 
                {
                    scope:this, 
                    success:this._querySuccess, 
                    failure:this._queryFailure, 
                    argument: {transformer:transformer, callback:callback}
                },
                this.queryParameters());
        } else {
            throw("HTTP methods other than GET and POST are not supported.");
        }
    };

    
    //----------
    // accessors
    this.request = function() { return _conn; };
    this.service = function() { return _service; };
    this.defaultGraphs = function() { return _default_graphs; };
    this.namedGraphs = function() { return _named_graphs; };
    this.prefixes = function() { return _prefix_map; };
    this.method = function() { return _method; };


    /**
     * Returns the SPARQL query represented by this object. The parameter, which can
     * be omitted, determines whether or not auto-generated PREFIX clauses are included
     * in the returned query string.
     */
    this.queryString = function(excludePrefixes) {
        var preamble = '';
        if (!excludePrefixes) {
            for (var prefix in this.prefixes()) {
                if(typeof(this.prefixes()[prefix]) != 'string') continue;
                preamble += 'PREFIX ' + prefix + ': <' + this.prefixes()[prefix] + '> ';
            }
        }
        return preamble + _user_query;
    };
    
    /**
     * Returns the HTTP query parameters to invoke this query. This includes entries for
     * all of the default graphs, the named graphs, the SPARQL query itself, and an 
     * output parameter to specify JSON (or other) output is desired.
     */
    this.queryParameters = function () {
        var urlQueryString = '';
        var i;
        
        // add default and named graphs to the protocol invocation
        for (i = 0; i < this.defaultGraphs().length; i++) urlQueryString += 'default-graph-uri=' + escape(this.defaultGraphs()[i]) + '&';
        for (i = 0; i < this.namedGraphs().length; i++) urlQueryString += 'named-graph-uri=' + escape(this.namedGraphs()[i]) + '&';
        
        // specify JSON output (currently output= supported by latest Joseki) (or other output)
        urlQueryString += 'output=' + _output + '&';
        return urlQueryString + 'query=' + escape(this.queryString());
    }
    
    /**
     * Returns the HTTP GET URL to invoke this query. (Note that this returns a full HTTP GET URL 
     * even if this query is set to actually use POST.)
     */
    this.queryUrl = function() {
        var url = this.service().endpoint() + '?';
        return url + this.queryParameters();
    };
    
    //---------
    // mutators
    function _add_graphs(toAdd, arr) {
        if (toAdd instanceof Array)
            for (var i = 0; i < toAdd.length; i++) arr.push(toAdd[i]);
        else
            arr.push(toAdd);
    }
    this.addDefaultGraph = function(g) { _add_graphs(g, this.defaultGraphs()); };
    this.addNamedGraph = function(g) { _add_graphs(g, this.namedGraphs()); };
    this.setPrefix = function(p, u) { this.prefixes()[p] = u; };
    this.setMethod = function(m) {
        if (m != 'GET' && m != 'POST') throw("HTTP methods other than GET and POST are not supported.");
        _method = m;
    };

    //---------------
    // public methods

    // use our varied transformations to create the various methods of actually issuing 
    // queries
    for (var query_form in SPARQL._query_transformations) {
        // need the extra function to properly scope query_form (qf)
        this[query_form] = (function(qf) {
            return function(queryString, callback) {
                this._doQuery(queryString, callback, SPARQL._query_transformations[qf]);
            };
        })(query_form);
    }
    

    //------------
    // constructor
    
    return this;
}

// Nothing to see here, yet.
SPARQL.QueryUtilities = {
};
