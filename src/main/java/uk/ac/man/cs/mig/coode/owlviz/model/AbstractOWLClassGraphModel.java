package uk.ac.man.cs.mig.coode.owlviz.model;

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
public class AbstractOWLClassGraphModel extends AbstractGraphModel {

    private OWLModelManager owlModelManager;

    private OWLObjectHierarchyProvider provider;

    private OWLObjectHierarchyProviderListener listener;

    private OWLOntologyChangeListener changeListener;

    private OWLModelManagerListener owlModelManagerListener;

    private static String relation_namespace = "http://relations/csis.pace.edu/semweb";    

    public AbstractOWLClassGraphModel(OWLModelManager owlModelManager,
                                      OWLObjectHierarchyProvider provider) {
        this.owlModelManager = owlModelManager;
        listener = new OWLObjectHierarchyProviderListener() {

            public void nodeChanged(OWLObject node) {
                // TODO: Sync!
            }


            public void childParentAdded(OWLObject child, OWLObject parent) {
                fireChildAddedEvent(parent, child);
                fireParentAddedEvent(child, parent);
            }

            public void childParentRemoved(OWLObject child, OWLObject parent) {
                fireChildRemovedEvent(parent, child);
                fireParentRemovedEvent(child, parent);
            }

            public void rootAdded(OWLObject root) {

            }

            public void rootRemoved(OWLObject root) {
            }

            public void hierarchyChanged() {
            }
        };
        provider.addListener(listener);
        this.provider = provider;
        changeListener = new OWLOntologyChangeListener() {
            public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
            }
        };
        owlModelManager.addOntologyChangeListener(changeListener);
        owlModelManagerListener = new OWLModelManagerListener() {
            public void handleChange(OWLModelManagerChangeEvent event) {
                if(event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED) || event.isType(EventType.ONTOLOGY_RELOADED)) {
                    // Clear
                    fireModelChangedEvent();
                }
            }
        };
        owlModelManager.addListener(owlModelManagerListener);
    }

    public void dispose() {
        provider.removeListener(listener);
        owlModelManager.removeOntologyChangeListener(changeListener);
        owlModelManager.removeListener(owlModelManagerListener);
    }

    protected Set<OWLObject> getChildren(OWLObject obj) {
        Set<OWLObject> children = new HashSet<OWLObject>();
            children.addAll(provider.getChildren(obj));
            children.addAll(provider.getEquivalents(obj));
        OWLOntology current_ontology = owlModelManager.getActiveOntology();
        OWLClass concept = (OWLClass)obj;
        Set<OWLClass> classes = current_ontology.getClassesInSignature();
        Set<OWLAnnotation> annotations = concept.getAnnotations(current_ontology);
        Iterator<OWLAnnotation> itr = annotations.iterator();
        while(itr.hasNext()) {
            OWLAnnotation annotation = (OWLAnnotation) itr.next();
            String delims = "[#]";
            String relation_value = annotation.getValue().toString();
                String[] value_tokens = relation_value.split(delims);
            String relation_property = annotation.getProperty().toString();
                String[] property_tokens = relation_property.split(delims);
                String property = property_tokens[0];
                property = property.substring(1,property.length());
            if(value_tokens.length > 1 && property.compareTo(relation_namespace)==0) {
                Iterator<OWLClass> itr2 = classes.iterator();
                while(itr2.hasNext()) {
                    OWLClass tempClass = (OWLClass) itr2.next();
                    String class_iri = tempClass.getIRI().toString();
                    if(relation_value.compareTo(class_iri)==0) {
                        children.add((OWLObject)tempClass);
                        break;
                    }
                }
            }
        }
        return children;
    }


    protected Set<OWLObject> getParents(OWLObject obj) {
        Set<OWLObject> parents = new HashSet<OWLObject>();
//            parents.addAll(provider.getParents(obj));
//            parents.addAll(provider.getEquivalents(obj));
        return parents;
    }

    public int getChildCount(Object obj) {
        return getChildren((OWLObject) obj).size();
    }

    public Iterator getChildren(Object obj) {
        return getChildren((OWLObject) obj).iterator();
    }

    public int getParentCount(Object obj) {
        return getParents((OWLObject) obj).size();
    }

    public Iterator getParents(Object obj) {
        return getParents((OWLObject) obj).iterator();
    }

    public boolean contains(Object obj) {
            if(obj instanceof OWLClass) {
                for(OWLOntology ont : owlModelManager.getActiveOntologies()) {
                    if(ont.containsClassInSignature(((OWLClass) obj).getIRI())) {
                        return true;
                    }
                }
            }
        return false;
    }

    public Object getRelationshipType(Object parentObject, Object childObject) {
        OWLOntology current_ontology = owlModelManager.getActiveOntology();
        OWLClass parentConcept = (OWLClass)parentObject;
        OWLClass childConcept = (OWLClass)childObject;
            String childName = childConcept.getIRI().toString();
        Set<OWLAnnotation> annotations = parentConcept.getAnnotations(current_ontology);
        Iterator<OWLAnnotation> itr = annotations.iterator();
        while(itr.hasNext()) {
            OWLAnnotation annotation = (OWLAnnotation) itr.next();
            String relation_value = annotation.getValue().toString();
            if(relation_value.compareTo(childName)!=0)
                continue;
            String delims = "[#]";
            String relation_property = annotation.getProperty().toString();
                String[] property_tokens = relation_property.split(delims);
                String property = property_tokens[1];
                property = property.substring(0,property.length()-1);
            return " " + property + " ";
        }
        return " is-a ";
    }

    public int getRelationshipDirection(Object parentObject, Object childObject) {
        return GraphModel.DIRECTION_BACK;
    }

    public Iterator getRelatedObjectsToAdd(Object obj) {
        return Collections.EMPTY_LIST.iterator();
    }

    public Iterator getRelatedObjectsToRemove(Object obj) {
        return Collections.EMPTY_LIST.iterator();
    }

}
