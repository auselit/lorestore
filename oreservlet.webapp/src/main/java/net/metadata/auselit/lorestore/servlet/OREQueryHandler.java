package net.metadata.auselit.lorestore.servlet;

import static net.metadata.auselit.lorestore.common.OREConstants.SPARQL_RESULTS_XML;

import java.io.ByteArrayOutputStream;

import net.metadata.auselit.lorestore.triplestore.TripleStoreConnectorFactory;

import org.apache.log4j.Logger;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.node.URI;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The OREQueryHandler class handles queries from the {@link OREController}.
 * 
 * @author uqdayers
 */
public class OREQueryHandler {
    private static final Logger LOG = Logger.getLogger(OREQueryHandler.class);
	protected final OREControllerConfig occ;
	private TripleStoreConnectorFactory cf;

	public OREQueryHandler(OREControllerConfig occ) {
		this.occ = occ;
		this.cf = occ.getContainerFactory();
	}

//	public OREResponse plainGet(HttpServletRequest request)
//			throws RepositoryException, RequestFailureException,
//			ServletException {
//		String stringURI = request.getRequestURI();
//
//		ModelSet container = cf.retrieveConnection();
//
//		URI uri = container.createURI(stringURI);
//		Model model = container.getModel(uri);
//
//		if (model == null || model.isEmpty()) {
//			LOG.debug("Requested object that doesn't exist");
//			throw new NoSuchRequestHandlingMethodException(request);
//			// throw new RequestFailureException(
//			// HttpServletResponse.SC_NOT_FOUND,
//			// "No resource for '" + uri + "'");
//		}
//		occ.getAccessPolicy().checkRead(request, model);
//
//		return new OREResponse(model);
//
//	}

	public ResponseEntity<String> browseQuery(String url) throws RepositoryException, MalformedQueryException, QueryEvaluationException, TupleQueryResultHandlerException {
		String queryString = generateBrowseQuery(url);
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.parseMediaType(SPARQL_RESULTS_XML));
		return new ResponseEntity<String>(runSparqlQuery(queryString), responseHeaders, HttpStatus.OK);
	}

	public ResponseEntity<String> exploreQuery(String url) throws RepositoryException, MalformedQueryException, QueryEvaluationException, TupleQueryResultHandlerException {
		String queryString = generateExploreQuery(url);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.parseMediaType(SPARQL_RESULTS_XML));
		return new ResponseEntity<String>(runSparqlQuery(queryString), responseHeaders, HttpStatus.OK);
	}
	
	
	private String runSparqlQuery(String queryString) throws RepositoryException, MalformedQueryException, QueryEvaluationException, TupleQueryResultHandlerException  {
		System.out.println("Query String: " + queryString);
		ModelSet container = cf.retrieveConnection();
		Repository rep = (Repository)container.getUnderlyingModelSetImplementation();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RepositoryConnection con = rep.getConnection();
		try {
//			queryString = "select * where { ?s ?p ?o. } ";
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			
			SPARQLResultsXMLWriter resultsXMLWriter = new SPARQLResultsXMLWriter(stream);
			
			tupleQuery.evaluate(resultsXMLWriter);
		} finally {
			con.close();
		}
		
		return stream.toString();
	}
	/*
select distinct ?g ?a ?m ?t
where {
   graph ?g  {
          {<escapedURL> ?p ?o .}
    UNION {?s ?p2 <escapedURL>}
    UNION {<altURL> ?p3 ?o2 .}
    UNION {?s2 ?p4 <altURL>}
  } 
  . {?g <http://purl.org/dc/elements/1.1/creator> ?a}
  . {?g <http://purl.org/dc/terms/modified> ?m}
  . OPTIONAL {?g <http://purl.org/dc/elements/1.1/title> ?t}
};
	 */
	protected String generateBrowseQuery(String escapedURL) {
		// Needs to match both www and non-www version of URL
		String altURL = makeAltURL(escapedURL);
		String query = "select distinct ?g ?a ?m ?t" + " where { graph ?g {"
				+ "{<" + escapedURL + "> ?p ?o .}" + " UNION " + "{?s ?p2 <"
				+ escapedURL + ">}" + " UNION " + "{<" + altURL
				+ "> ?p3 ?o2 .}" + " UNION " + "{?s2 ?p4 <" + altURL + ">}} "
				+ ". {?g <http://purl.org/dc/elements/1.1/creator> ?a}"
				+ ". {?g <http://purl.org/dc/terms/modified> ?m}"
				+ ". OPTIONAL {?g <http://purl.org/dc/elements/1.1/title> ?t}}";

		return query;
	}

	protected String makeAltURL(String url) {
		// TODO needs to escape
		if (url.contains("http://www.")) {
			return url.replace("http://www.", "http://");
		} else {
			return url.replace("http://", "http://www.");
		}
	}

	/**
	 * Constructs a filter for a SPARQL query
	 * 
	 * @param matchVal
	 * @return
	 */
	protected String makeFilter(String matchVal) {
		// implicit and, use quotes for phrase search
		String fExpr = "";
		if (matchVal.startsWith("\"") && matchVal.endsWith("\"")) {
			String temp = matchVal.substring(1, matchVal.length() - 1);
			fExpr = "FILTER regex(str(?v?, \"" + temp + "\", \"i\")";
		} else if (matchVal.contains(" ")) {
			for (String term : matchVal.split(" ")) {
				fExpr += "FILTER regex(str(?v), \"" + term + "\", \"i\"). ";
			}
		} else {
			fExpr = "FILTER regex(str(?v), \"" + matchVal + "\", \"i\")";
		}
		return fExpr;
	}

	public OREResponse getOreObject(String oreId) {
		ModelSet container = cf.retrieveConnection();

		URI uri = container.createURI(occ.getBaseUri() + oreId);
		Model model = container.getModel(uri);
		if (model == null || model.isEmpty()) {
			LOG.debug("Cannot find requested resource: " + oreId);
			throw new RuntimeException("Can't find resource: " + uri);
			// throw new RequestFailureException(
			// HttpServletResponse.SC_NOT_FOUND,
			// "No resource for '" + uri + "'");
		}
		// occ.getAccessPolicy().checkRead(request, model);

		return new OREResponse(model);

	}
	
	protected String generateExploreQuery(String escapedURI) {
		String query = "PREFIX dc:<http://purl.org/dc/elements/1.1/> " 
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "PREFIX ore:<http://www.openarchives.org/ore/terms/> " 
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
		    + "SELECT DISTINCT ?something ?somerel ?sometitle ?sometype ?creator ?modified ?anotherrel ?somethingelse WHERE {"
            // Compound objects that contain this uri
		    + "{?aggre ore:aggregates <" + escapedURI + "> . " 
                    + "?something ore:describes ?aggre . "
		            + "?something a ?sometype . " 
                    + "OPTIONAL {?something dc:creator ?creator .} "
                    + "OPTIONAL {?something dcterms:modified ?modified .} "
                    + "OPTIONAL {?something dc:title ?sometitle .}}"
            // uris that have an asserted relationship to this uri
		    +  "UNION { ?something ?somerel <" + escapedURI + "> . " 
                    + "FILTER isURI(?something) ."
		            + "FILTER (?somerel != ore:aggregates) . " 
                    + "FILTER (?somerel != rdf:type) . " 
                    + "OPTIONAL {?something a ?sometype} ."
                    + "OPTIONAL {?something dc:title ?sometitle.} }"
            // uris that have an asserted relationships from this uri
		    + "UNION {<"+ escapedURI + "> ?somerel ?something . " 
                    + "FILTER isURI(?something). " 
                    + "FILTER (?somerel != rdf:type) . " 
                    + "FILTER (?somerel != ore:describes) . "
                    + "OPTIONAL {?something a ?sometype} ."
                    + "OPTIONAL {?something dc:title ?sometitle.}}"
            // if this is a compound object, uris contained
		    + "UNION {<" + escapedURI + "> ore:describes ?aggre ."
                    + "?aggre ?somerel ?something . " 
                    + "FILTER isURI(?something) ."
                    + "FILTER (?somerel != rdf:type) ." 
                    + "OPTIONAL {?something dc:title ?sometitle . } . " 
                    + "OPTIONAL {?something ?anotherrel ?somethingelse . FILTER isURI(?somethingelse)} . "
                    + "OPTIONAL {?something a ?sometype}}}";
		return query;
	}
	
}