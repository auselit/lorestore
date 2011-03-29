package oreservlet.model;

import static oreservlet.common.OREConstants.AGGREGATION;
import static oreservlet.common.OREConstants.ORE_DESCRIBES_PROPERTY;
import static oreservlet.common.OREConstants.ORE_RESOURCEMAP_CLASS;
import static oreservlet.common.OREConstants.RDF_TYPE_PROPERTY;
import oreservlet.exceptions.OREException;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;

public class CompoundObjectImpl {

	private Model model;

	public CompoundObjectImpl(Model model) {
		this.model = model;
	}

	public URI getContextURI() {
		return model.getContextURI();
	}

	public boolean isOpen() {
		return model.isOpen();
	}

	public Model getModel() {
		return model;
	}

	public String getModelAsRDFXML() throws OREException {
		return model.serialize(Syntax.RdfXml);
	}

	public String getResourceMapURL() throws OREException {
		return findResourceMap().toString();
	}

	/**
	 * Assigns a new URI to the embedded model, updating the URI of the
	 * ResourceMap and the associated Aggregation
	 * 
	 * @param newUriString
	 * @throws OREException
	 */
	public void assignURI(String newUriString) throws OREException {
		Resource oldUri = findResourceMap();
		model.setAutocommit(false);
		URI newURI = model.createURI(newUriString);
		ClosableIterator<Statement> resourceMapStmts = model.findStatements(
				oldUri, Variable.ANY, Variable.ANY);
		updateSubjectURI(resourceMapStmts, newURI);
		updateAggregationURI(newURI);
		model.commit();
	}

	private void updateAggregationURI(Resource resourceMap) throws OREException {
		URI newAggregationURI = model.createURI(resourceMap.toString()
				+ AGGREGATION);

		// Find all the aggregations referenced by this ResourceMap
		ClosableIterator<Statement> aggregations = model.findStatements(
				resourceMap, model.createURI(ORE_DESCRIBES_PROPERTY),
				Variable.ANY);

		while (aggregations.hasNext()) {
			Statement s = aggregations.next();
			URI aggregationURI = s.getObject().asURI();

			model.addStatement(s.getSubject(), s.getPredicate(),
					newAggregationURI);
			model.removeStatement(s);
			
			// Find all the statements referenced by this Aggregation
			ClosableIterator<Statement> aggregationStmts = model
					.findStatements(aggregationURI, Variable.ANY, Variable.ANY);
			updateSubjectURI(aggregationStmts, newAggregationURI);
		}
	}

	private void updateSubjectURI(ClosableIterator<Statement> it, URI newURI) {
		while (it.hasNext()) {
			Statement s = it.next();

			model.addStatement(newURI, s.getPredicate(), s.getObject());
			model.removeStatement(s);
		}
		it.close();
	}

	private Resource findResourceMap() throws OREException {
		ClosableIterator<Statement> it = model.findStatements(Variable.ANY,
				model.createURI(RDF_TYPE_PROPERTY),
				model.createURI(ORE_RESOURCEMAP_CLASS));
		Statement s = null;
		if (it.hasNext()) {
			s = it.next();
		}
		if (s == null) {
			throw new OREException("Appears not to be an ORE ResourceMap");
		}
		if (it.hasNext()) {
			throw new OREException("Multiple ResourceMaps");
		}
		it.close();
		return s.getSubject();
	}

}
