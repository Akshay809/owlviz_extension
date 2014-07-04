package uk.ac.man.cs.mig.coode.owlviz.model;

import org.semanticweb.owlapi.model.IRI;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProviderListener;
import org.semanticweb.owlapi.model.*;
import uk.ac.man.cs.mig.util.graph.model.GraphModel;
import uk.ac.man.cs.mig.util.graph.model.impl.AbstractGraphModel;

import java.util.*;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 08-Jun-2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public class OWLClassGraphAssertedModel extends AbstractOWLClassGraphModel {

    private OWLObjectHierarchyProvider provider;

    private OWLModelManager owlModelManager;

    private static Map<String, Map<String, String>> mapper;

	private static String relation_namespace = "http://csis.pace.edu/semweb/relationship";    

    public OWLClassGraphAssertedModel(OWLModelManager owlModelManager) {
        super(owlModelManager, owlModelManager.getOWLHierarchyManager().getOWLClassHierarchyProvider());
        this.mapper = new HashMap<String, Map<String, String>>();
        this.provider = owlModelManager.getOWLHierarchyManager().getOWLClassHierarchyProvider();
        this.owlModelManager = owlModelManager;
    }


    @Override
    protected Set<OWLObject> getChildren(OWLObject obj) {
        OWLOntology current_ontology = owlModelManager.getActiveOntology();
        Set<OWLObject> children = new HashSet<OWLObject>();
            children.addAll(provider.getChildren(obj));
            children.addAll(provider.getEquivalents(obj));

//        Set<OWLClass> classes = current_ontology.getClassesInSignature();
        OWLClass concept = (OWLClass)obj;
        Set<OWLAnnotation> annotations = concept.getAnnotations(current_ontology);
        Iterator<OWLAnnotation> itr = annotations.iterator();
        OWLAnnotation annotation;
        Set<OWLClass> classes = current_ontology.getClassesInSignature();
        String delims = "[#]";
        String relation_on = concept.getIRI().toString();
        String relation_value, relation_property, connected_on, property_prefix;
        while(itr.hasNext()) {
            annotation = (OWLAnnotation) itr.next();
            relation_value = annotation.getValue().toString();
                String[] value_tokens = relation_value.split(delims);
            relation_property = annotation.getProperty().toString();
                String[] property_tokens = relation_property.split(delims);
            connected_on = property_tokens[1];
	            connected_on = connected_on.substring(0,connected_on.length()-1);
            property_prefix = property_tokens[0];
	            property_prefix = property_prefix.substring(1,property_prefix.length());
                //Connecting...
                //(KEY1)  relation_on
                //(KEY2)  relation_value
                //(VALUE) connected_on
            if(value_tokens.length > 1 && property_prefix.compareTo(relation_namespace)==0) {
                Iterator<OWLClass> itr2 = classes.iterator();
//            	IRI iri = concept.getIRI();
//				OWLClass tempClass = (OWLClass)current_ontology.getEntitiesInSignature(iri.create(relation_value));
                while(itr2.hasNext()) {
                    OWLClass tempClass = (OWLClass) itr2.next();
                    String class_iri = tempClass.getIRI().toString();
                    if(relation_value.compareTo(class_iri)==0) {
		                children.add((OWLObject)tempClass);
		                if(mapper.isEmpty()) {
		                    Map<String, String> base_map = new HashMap<String, String>();
		                    base_map.put(relation_value, connected_on);
		                    mapper.put(relation_on,base_map);
		                }
		                else {
		                    if(mapper.containsKey(relation_on)) {
		                        Map<String, String> temp_map = mapper.get(relation_on);
		                        temp_map.put(relation_value, connected_on);
		                    }
		                    else {
		                        Map<String, String> temp_map = new HashMap<String, String>();
		                        temp_map.put(relation_value, connected_on);
		                        mapper.put(relation_on,temp_map);
		                    }
		                }
	                    break;
    	            }
                }
            }
        }
        return children;
    }


    @Override
    protected Set<OWLObject> getParents(OWLObject obj) {
        Set<OWLObject> parents = new HashSet<OWLObject>();
        return parents;
    }


    @Override
    public Object getRelationshipType(Object parentObject, Object childObject) {
        OWLClass parentConcept = (OWLClass)parentObject;
        OWLClass childConcept = (OWLClass)childObject;
        String relation_on = parentConcept.getIRI().toString();
        String relation_to = childConcept.getIRI().toString();
        if(mapper.containsKey(relation_on))
            if(mapper.get(relation_on).containsKey(relation_to))
                return " " + mapper.get(relation_on).get(relation_to) + " ";
        return " is-a ";
    }


    @Override
    public int getRelationshipDirection(Object parentObject, Object childObject) {
        Set<OWLObject> parents = provider.getParents((OWLObject)childObject);
        Iterator<OWLObject> itr = parents.iterator();
        while(itr.hasNext()) {
            OWLObject parent = (OWLObject) itr.next();
            if(parent.compareTo((OWLObject)parentObject)==0)
                return GraphModel.DIRECTION_BACK;
        }
        return GraphModel.DIRECTION_FORWARD;
    }
}