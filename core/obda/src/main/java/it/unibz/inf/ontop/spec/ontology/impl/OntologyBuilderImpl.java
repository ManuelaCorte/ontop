package it.unibz.inf.ontop.spec.ontology.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.term.Constant;
import it.unibz.inf.ontop.model.term.ObjectConstant;
import it.unibz.inf.ontop.model.term.ValueConstant;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.spec.ontology.*;

import java.util.*;
import java.util.function.Function;

public class OntologyBuilderImpl implements OntologyBuilder {

    private final Hierarchy<ClassExpression> classAxioms = new Hierarchy<>();
    private final Hierarchy<ObjectPropertyExpression> objectPropertyAxioms = new Hierarchy<>();
    private final Hierarchy<DataPropertyExpression> dataPropertyAxioms = new Hierarchy<>();

    private final ImmutableList.Builder<BinaryAxiom<DataRangeExpression>> subDataRangeAxioms = ImmutableList.builder();

    private final ImmutableSet.Builder<ObjectPropertyExpression> reflexiveObjectPropertyAxioms = ImmutableSet.builder();
    private final ImmutableSet.Builder<ObjectPropertyExpression> irreflexiveObjectPropertyAxioms = ImmutableSet.builder();

    private final ImmutableSet.Builder<ObjectPropertyExpression> functionalObjectPropertyAxioms = ImmutableSet.builder();
    private final ImmutableSet.Builder<DataPropertyExpression> functionalDataPropertyAxioms = ImmutableSet.builder();

    // exception messages

    private static final String CLASS_NOT_FOUND = "Class not found: ";
    private static final String OBJECT_PROPERTY_NOT_FOUND = "ObjectProperty not found: ";
    private static final String DATA_PROPERTY_NOT_FOUND = "DataProperty not found: ";
    private static final String DATATYPE_NOT_FOUND = "Datatype not found: ";
    private static final String ANNOTATION_PROPERTY_NOT_FOUND = "AnnotationProperty not found: ";

    private final OntologyCategoryImpl<OClass> classes =
            new OntologyCategoryImpl<>(ClassImpl::new, CLASS_NOT_FOUND,"");
    private final OntologyCategoryImpl<ObjectPropertyExpression> objectProperties =
            new OntologyCategoryImpl<>(ObjectPropertyExpressionImpl::new, OBJECT_PROPERTY_NOT_FOUND,"");
    private final OntologyCategoryImpl<DataPropertyExpression> dataProperties =
            new OntologyCategoryImpl<>(DataPropertyExpressionImpl::new, DATA_PROPERTY_NOT_FOUND,"");
    private final OntologyCategoryImpl<AnnotationProperty> annotationProperties =
            new OntologyCategoryImpl<>(AnnotationPropertyImpl::new, ANNOTATION_PROPERTY_NOT_FOUND,"");

    // assertions

    private final ImmutableList.Builder<ClassAssertion> classAssertions = ImmutableList.builder();
    private final ImmutableList.Builder<ObjectPropertyAssertion> objectPropertyAssertions = ImmutableList.builder();
    private final ImmutableList.Builder<DataPropertyAssertion> dataPropertyAssertions = ImmutableList.builder();
    private final ImmutableList.Builder<AnnotationAssertion> annotationAssertions = ImmutableList.builder();

    private OntologyBuilderImpl() {
        classes.map.put(ClassImpl.owlThingIRI, ClassImpl.owlThing);
        classes.map.put(ClassImpl.owlNothingIRI, ClassImpl.owlNothing);
        objectProperties.map.put(ObjectPropertyExpressionImpl.owlTopObjectPropertyIRI, ObjectPropertyExpressionImpl.owlTopObjectProperty);
        objectProperties.map.put(ObjectPropertyExpressionImpl.owlBottomObjectPropertyIRI, ObjectPropertyExpressionImpl.owlBottomObjectProperty);
        dataProperties.map.put(DataPropertyExpressionImpl.owlTopDataPropertyIRI, DataPropertyExpressionImpl.owlTopDataProperty);
        dataProperties.map.put(DataPropertyExpressionImpl.owlBottomDataPropertyIRI, DataPropertyExpressionImpl.owlBottomDataProperty);
    }

    public static OntologyBuilder builder() {
        return new OntologyBuilderImpl();
    }

    public static ABoxAssertionSupplier assertionSupplier() {
        return new ABoxAssertionSupplier() {
            @Override
            public ClassAssertion createClassAssertion(String c, ObjectConstant o) throws InconsistentOntologyException {
                return OntologyBuilderImpl.createClassAssertion(new ClassImpl(c), o);
            }

            @Override
            public ObjectPropertyAssertion createObjectPropertyAssertion(String op, ObjectConstant o1, ObjectConstant o2) throws InconsistentOntologyException {
                return OntologyBuilderImpl.createObjectPropertyAssertion(new ObjectPropertyExpressionImpl(op), o1, o2);
            }

            @Override
            public DataPropertyAssertion createDataPropertyAssertion(String dp, ObjectConstant o, ValueConstant v) throws InconsistentOntologyException {
                return OntologyBuilderImpl.createDataPropertyAssertion(new DataPropertyExpressionImpl(dp), o, v);
            }
        };
    }




    static final class OntologyCategoryImpl<T> implements OntologyVocabularyCategory<T> {
        private final Map<String, T> map = new HashMap<>();

        private final String NOT_FOUND, EXISTS;
        private final Function<String, ? extends T> ctor;

        OntologyCategoryImpl(Function<String, ? extends T> ctor, String NOT_FOUND, String EXISTS) {
            this.ctor = ctor;
            this.NOT_FOUND = NOT_FOUND;
            this.EXISTS = EXISTS;
        }

        @Override
        public T get(String uri) {
            T oc = map.get(uri);
            if (oc == null)
                throw new RuntimeException(NOT_FOUND + uri);
            return oc;
        }

        @Override
        public boolean contains(String uri) {
            return map.containsKey(uri);
        }

        @Override
        public Iterator<T> iterator() {
            return map.values().iterator();
        }

        private T create(String uri) {
            // TODO: check for built-in
            //if (map.containsKey(uri))
            //    throw new RuntimeException(EXISTS + uri);
            T n = ctor.apply(uri);
            map.put(uri, n);
            return n;
        }

        OntologyImpl.ImmutableOntologyVocabularyCategoryImpl<T> getImmutableCopy() {
            return new OntologyImpl.ImmutableOntologyVocabularyCategoryImpl<T>(ImmutableMap.copyOf(map), NOT_FOUND);
        }
    }




    @Override
    public OntologyVocabularyCategory<OClass> classes() { return classes; }

    @Override
    public OntologyVocabularyCategory<ObjectPropertyExpression> objectProperties() { return objectProperties; }

    @Override
    public OntologyVocabularyCategory<DataPropertyExpression> dataProperties() { return dataProperties; }

    @Override
    public OntologyVocabularyCategory<AnnotationProperty> annotationProperties() { return annotationProperties; }


    @Override
    public OClass declareClass(String uri) {
        return classes.create(uri);
    }

    @Override
    public ObjectPropertyExpression declareObjectProperty(String uri) {
        return objectProperties.create(uri);
    }

    @Override
    public DataPropertyExpression declareDataProperty(String uri) {
        return dataProperties.create(uri);
    }

    @Override
    public AnnotationProperty declareAnnotationProperty(String uri) {
        return annotationProperties.create(uri);
    }

    @Override
    public Datatype getDatatype(String uri) {
        Datatype dt = OntologyImpl.OWL2QLDatatypes.get(uri);
        if (dt == null)
            throw new RuntimeException(DATATYPE_NOT_FOUND + uri);
        return dt;
    }

    /**
     * Normalizes and adds subclass axiom
     * <p>
     * SubClassOf := 'SubClassOf' '(' axiomAnnotations subClassExpression superClassExpression ')'
     * <p>
     * Implements rule [C1]:<br>
     *    - ignore the axiom if the first argument is owl:Nothing or the second argument is owl:Thing<br>
     *    - replace by a disjointness axiom if the second argument is owl:Nothing but the first is not owl:Thing<br>
     *    - inconsistency if the first argument is owl:Thing but the second one is not owl:Nothing
     * <p>
     * Implements rules [D5] and [O5] (in conjunction with DataSomeValuesFromImpl and ObjectSomeValuesFromImpl)<br>
     *    - if the first argument is syntactically "equivalent" to owl:Thing, then replace it by owl:Thing
     *
     * @throws InconsistentOntologyException
     */

    @Override
    public void addSubClassOfAxiom(ClassExpression ce1, ClassExpression ce2) throws InconsistentOntologyException {
        checkSignature(ce1);
        checkSignature(ce2);
        if (ce1.isTop())
            ce1 = ClassImpl.owlThing; // rules [D5] and [O5]
        classAxioms.addInclusion(ce1, ce2);
    }

    /**
     * Normalizes and adds a data property range axiom
     * <p>
     * DataPropertyRange := 'DataPropertyRange' '(' axiomAnnotations DataPropertyExpression DataRange ')'
     * <p>
     * Implements rule [D3]:
     *     - ignore if the property is bot or the range is rdfs:Literal (top datatype)
     *     - inconsistency if the property is top but the range is not rdfs:Literal
     *
     * @throws InconsistentOntologyException
     */

    @Override
    public void addDataPropertyRangeAxiom(DataPropertyRangeExpression range, Datatype datatype) throws InconsistentOntologyException {
        checkSignature(range);
        checkSignature(datatype);
        if (datatype.equals(DatatypeImpl.rdfsLiteral))
            return;

        // otherwise the datatype is not top
        if (range.getProperty().isBottom())
            return;
        if (range.getProperty().isTop())
            throw new InconsistentOntologyException();

        BinaryAxiom<DataRangeExpression> ax = new BinaryAxiomImpl<>(range, datatype);
        subDataRangeAxioms.add(ax);
    }


    /**
     * Normalizes and adds an object subproperty axiom
     * <p>
     * SubObjectPropertyOf := 'SubObjectPropertyOf' '(' axiomAnnotations
     * 						ObjectPropertyExpression ObjectPropertyExpression ')'
     * <p>
     * Implements rule [O1]:<br>
     *    - ignore the axiom if the first argument is owl:bottomObjectProperty
     *    				or the second argument is owl:topObjectProperty<br>
     *    - replace by a disjointness axiom if the second argument is owl:bottomObjectProperty
     *                but the first one is not owl:topObjectProperty<br>
     *    - inconsistency if the first is  owl:topObjectProperty but the second is owl:bottomObjectProperty
     *
     * @throws InconsistentOntologyException
     *
     */

    @Override
    public void addSubPropertyOfAxiom(ObjectPropertyExpression ope1, ObjectPropertyExpression ope2) throws InconsistentOntologyException {
        checkSignature(ope1);
        checkSignature(ope2);
        objectPropertyAxioms.addInclusion(ope1, ope2);
    }

    /**
     * Normalizes and adds a data subproperty axiom
     * <p>
     * SubDataPropertyOf := 'SubDataPropertyOf' '(' axiomAnnotations
     * 					subDataPropertyExpression superDataPropertyExpression ')'<br>
     * subDataPropertyExpression := DataPropertyExpression<br>
     * superDataPropertyExpression := DataPropertyExpression
     * <p>
     * implements rule [D1]:<br>
     *    - ignore the axiom if the first argument is owl:bottomDataProperty
     *    			  or the second argument is owl:topDataProperty<br>
     *    - replace by a disjointness axiom if the second argument is owl:bottomDataProperty
     *                but the first one is not owl:topDataProperty<br>
     *    - inconsistency if the first is  owl:topDataProperty but the second is owl:bottomDataProperty
     *
     * @throws InconsistentOntologyException
     */

    @Override
    public void addSubPropertyOfAxiom(DataPropertyExpression dpe1, DataPropertyExpression dpe2) throws InconsistentOntologyException {
        checkSignature(dpe1);
        checkSignature(dpe2);
        dataPropertyAxioms.addInclusion(dpe1, dpe2);
    }


    /**
     * Normalizes and adds class disjointness axiom
     * <p>
     * DisjointClasses := 'DisjointClasses' '(' axiomAnnotations
     * 			subClassExpression subClassExpression { subClassExpression } ')'<br>
     * <p>
     * Implements rule [C2]:<br>
     *     - eliminates all occurrences of bot and if the result contains<br>
     *     - no top and at least two elements then disjointness<br>
     *     - one top then emptiness of all other elements<br>
     *     - two tops then inconsistency (this behavior is an extension of OWL 2, where duplicates are removed from the list)
     */

    @Override
    public void addDisjointClassesAxiom(ClassExpression... ces) throws InconsistentOntologyException {
        for (ClassExpression c : ces)
            checkSignature(c);
        classAxioms.addDisjointness(ces);
    }

    /**
     * Normalizes and adds object property disjointness axiom
     * <p>
     * DisjointObjectProperties := 'DisjointObjectProperties' '(' axiomAnnotations
     * 		 ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'<br>
     * <p>
     * Implements rule [O2]:<br>
     *     - eliminates all occurrences of bot and if the result contains<br>
     *     - no top and at least two elements then disjointness<br>
     *     - one top then emptiness of all other elements<br>
     *     - two tops then inconsistency (this behavior is an extension of OWL 2, where duplicates are removed from the list)
     */

    @Override
    public void addDisjointObjectPropertiesAxiom(ObjectPropertyExpression... opes) throws InconsistentOntologyException {
        for (ObjectPropertyExpression p : opes)
            checkSignature(p);
        objectPropertyAxioms.addDisjointness(opes);
    }

    /**
     * Normalizes and adds data property disjointness axiom
     * <p>
     * DisjointDataProperties := 'DisjointDataProperties' '(' axiomAnnotations
     * 				DataPropertyExpression DataPropertyExpression { DataPropertyExpression } ')'<br>
     * <p>
     * Implements rule [D2]:<br>
     *     - eliminates all occurrences of bot and if the result contains<br>
     *     - no top and at least two elements then disjointness<br>
     *     - one top then emptiness of all other elements<br>
     *     - two tops then inconsistency (this behavior is an extension of OWL 2, where duplicates are removed from the list)
     */

    @Override
    public void addDisjointDataPropertiesAxiom(DataPropertyExpression... dpes) throws InconsistentOntologyException {
        for (DataPropertyExpression dpe : dpes)
            checkSignature(dpe);
        dataPropertyAxioms.addDisjointness(dpes);
    }


    /**
     * Normalizes and adds a reflexive object property axiom
     * <p>
     * ReflexiveObjectProperty := 'ReflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
     * <p>
     * Implements rule [O3]:<br>
     *     - ignores if top (which is reflexive by definition)<br>
     *     - inconsistency if bot (which is not reflexive)<br>
     *     - otherwise, removes the inverse if required
     *
     * @throws InconsistentOntologyException
     */

    @Override
    public void addReflexiveObjectPropertyAxiom(ObjectPropertyExpression ope) throws InconsistentOntologyException {
        if (ope.isTop())
            return;
        if (ope.isBottom())
            throw new InconsistentOntologyException();

        if (ope.isInverse())
            reflexiveObjectPropertyAxioms.add(ope.getInverse());
        else
            reflexiveObjectPropertyAxioms.add(ope);
    }

    /**
     * Normalizes and adds an irreflexive object property axiom
     * <p>
     * ReflexiveObjectProperty := 'ReflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
     * <p>
     * Implements rule [O3]:<br>
     *     - ignores if bot (which is irreflexive by definition)<br>
     *     - inconsistency if top (which is reflexive)<br>
     *     - otherwise, removes the inverse if required
     *
     * @throws InconsistentOntologyException
     */

    @Override
    public void addIrreflexiveObjectPropertyAxiom(ObjectPropertyExpression ope) throws InconsistentOntologyException {
        if (ope.isTop())
            throw new InconsistentOntologyException();
        if (ope.isBottom())
            return;

        if (ope.isInverse())
            irreflexiveObjectPropertyAxioms.add(ope.getInverse());
        else
            irreflexiveObjectPropertyAxioms.add(ope);
    }


    @Override
    public void addFunctionalObjectPropertyAxiom(ObjectPropertyExpression prop) {
        checkSignature(prop);
        functionalObjectPropertyAxioms.add(prop);
    }

    @Override
    public void addFunctionalDataPropertyAxiom(DataPropertyExpression prop) {
        checkSignature(prop);
        functionalDataPropertyAxioms.add(prop);
    }






    /**
     * Creates a class assertion
     * <p>
     * ClassAssertion := 'ClassAssertion' '(' axiomAnnotations Class Individual ')'
     * <p>
     * Implements rule [C4]:
     *     - ignore (return null) if the class is top
     *     - inconsistency if the class is bot
     */

    public static ClassAssertion createClassAssertion(OClass ce, ObjectConstant object) throws InconsistentOntologyException {
        if (ce.isTop())
            return null;
        if (ce.isBottom())
            throw new InconsistentOntologyException();

        return new ClassAssertionImpl(ce, object);
    }

    /**
     * Creates an object property assertion
     * <p>
     * ObjectPropertyAssertion := 'ObjectPropertyAssertion' '(' axiomAnnotations
     *				ObjectPropertyExpression sourceIndividual targetIndividual ')'
     * <p>
     * Implements rule [O4]:
     *     - ignore (return null) if the property is top
     *     - inconsistency if the property is bot
     *     - swap the arguments to eliminate inverses
     */

    public static ObjectPropertyAssertion createObjectPropertyAssertion(ObjectPropertyExpression ope, ObjectConstant o1, ObjectConstant o2) throws InconsistentOntologyException {
        if (ope.isTop())
            return null;
        if (ope.isBottom())
            throw new InconsistentOntologyException();

        if (ope.isInverse())
            return new ObjectPropertyAssertionImpl(ope.getInverse(), o2, o1);
        else
            return new ObjectPropertyAssertionImpl(ope, o1, o2);
    }

    /**
     * Creates a data property assertion
     * <p>
     * DataPropertyAssertion := 'DataPropertyAssertion' '(' axiomAnnotations
     * 					DataPropertyExpression sourceIndividual targetValue ')'
     * <p>
     * Implements rule [D4]:
     *     - ignore (return null) if the property is top
     *     - inconsistency if the property is bot
     */

    public static DataPropertyAssertion createDataPropertyAssertion(DataPropertyExpression dpe, ObjectConstant o1, ValueConstant o2) throws InconsistentOntologyException {
        if (dpe.isTop())
            return null;
        if (dpe.isBottom())
            throw new InconsistentOntologyException();

        return new DataPropertyAssertionImpl(dpe, o1, o2);
    }

    /**
     * Creates an annotation assertion
     * AnnotationAssertion := 'AnnotationAssertion' '(' axiomAnnotations AnnotationProperty AnnotationSubject AnnotationValue ')'
     * AnnotationSubject := IRI | AnonymousIndividual
     *
     */
    public static AnnotationAssertion createAnnotationAssertion(AnnotationProperty ap, ObjectConstant o, Constant c) {
        return new AnnotationAssertionImpl(ap,o,c);
    }



    @Override
    public void addClassAssertion(OClass ce, ObjectConstant o) throws InconsistentOntologyException {
        checkSignature(ce);
        ClassAssertion assertion = createClassAssertion(ce, o);
        if (assertion != null)
            classAssertions.add(assertion);
    }

    @Override
    public void addObjectPropertyAssertion(ObjectPropertyExpression ope, ObjectConstant o1, ObjectConstant o) throws InconsistentOntologyException {
        checkSignature(ope);
        ObjectPropertyAssertion assertion = createObjectPropertyAssertion(ope, o1, o);
        if (assertion != null)
            objectPropertyAssertions.add(assertion);
    }

    @Override
    public void addDataPropertyAssertion(DataPropertyExpression dpe, ObjectConstant o, ValueConstant v) throws InconsistentOntologyException {
        checkSignature(dpe);
        DataPropertyAssertion assertion = createDataPropertyAssertion(dpe, o, v);
        if (assertion != null)
            dataPropertyAssertions.add(assertion);
    }

    @Override
    public void addAnnotationAssertion(AnnotationProperty ap, ObjectConstant o, Constant c) {
        checkSignature(ap);
        AnnotationAssertion assertion = createAnnotationAssertion(ap, o, c);
        if (assertion != null)
            annotationAssertions.add(assertion);
    }



    @Override
    public Ontology build() {
        return new OntologyImpl(classes.getImmutableCopy(), objectProperties.getImmutableCopy(),
                ImmutableSet.copyOf(auxObjectProperties), dataProperties.getImmutableCopy(), annotationProperties.getImmutableCopy(),
                classAxioms.inclusions.build(), classAxioms.disjointness.build(),
                objectPropertyAxioms.inclusions.build(), objectPropertyAxioms.disjointness.build(),
                dataPropertyAxioms.inclusions.build(), dataPropertyAxioms.disjointness.build(),
                subDataRangeAxioms.build(), reflexiveObjectPropertyAxioms.build(), irreflexiveObjectPropertyAxioms.build(),
                functionalObjectPropertyAxioms.build(), functionalDataPropertyAxioms.build(),
                classAssertions.build(), objectPropertyAssertions.build(), dataPropertyAssertions.build(), annotationAssertions.build());
    }

    @Override
    public OntologyTBox buildUnclassifiedTBox() {
        return new OntologyImpl(classes.getImmutableCopy(), objectProperties.getImmutableCopy(),
                ImmutableSet.copyOf(auxObjectProperties), dataProperties.getImmutableCopy(), annotationProperties.getImmutableCopy(),
                classAxioms.inclusions.build(), classAxioms.disjointness.build(),
                objectPropertyAxioms.inclusions.build(), objectPropertyAxioms.disjointness.build(),
                dataPropertyAxioms.inclusions.build(), dataPropertyAxioms.disjointness.build(),
                subDataRangeAxioms.build(), reflexiveObjectPropertyAxioms.build(), irreflexiveObjectPropertyAxioms.build(),
                functionalObjectPropertyAxioms.build(), functionalDataPropertyAxioms.build(),
                classAssertions.build(), objectPropertyAssertions.build(), dataPropertyAssertions.build(), annotationAssertions.build()).unclassifiedTBox();
    }

    @Override
    public OntologyVocabulary buildVocabulary() {
        return new OntologyVocabularyImpl(classes.getImmutableCopy(), objectProperties.getImmutableCopy(),
                dataProperties.getImmutableCopy(), annotationProperties.getImmutableCopy());
    }

    // auxiliary symbols (for normalization)

    private final Set<ObjectPropertyExpression> auxObjectProperties = new HashSet<>();

    private static final String AUXROLEURI = "AUX.ROLE";
    private int auxCounter = 0;


    @Override
    public ObjectPropertyExpression createAuxiliaryObjectProperty() {
        ObjectPropertyExpression ope = new ObjectPropertyExpressionImpl(AUXROLEURI + auxCounter);
        auxCounter++ ;
        auxObjectProperties.add(ope);
        return ope;
    }



    private void checkSignature(ClassExpression desc) {
        if (desc instanceof OClass) {
            OClass cl = (OClass) desc;
            if (!classes.contains(cl.getName()))
                throw new IllegalArgumentException(CLASS_NOT_FOUND + desc);
        }
        else if (desc instanceof ObjectSomeValuesFrom) {
            checkSignature(((ObjectSomeValuesFrom) desc).getProperty());
        }
        else  {
            assert (desc instanceof DataSomeValuesFrom);
            checkSignature(((DataSomeValuesFrom) desc).getProperty());
        }
    }

    private void checkSignature(Datatype desc) {
        Predicate pred = desc.getPredicate();
        if (!OntologyImpl.OWL2QLDatatypes.containsKey(pred.getName()))
            throw new IllegalArgumentException(DATATYPE_NOT_FOUND + pred);
    }

    private void checkSignature(DataPropertyRangeExpression desc) {
        checkSignature(desc.getProperty());
    }

    private void checkSignature(ObjectPropertyExpression prop) {
        if (prop.isInverse())
            prop = prop.getInverse();

        if (!objectProperties.contains(prop.getName()) && !auxObjectProperties.contains(prop))
            throw new IllegalArgumentException(OBJECT_PROPERTY_NOT_FOUND + prop);
    }

    private void checkSignature(DataPropertyExpression prop) {
        if (!dataProperties.contains(prop.getName()))
            throw new IllegalArgumentException(DATA_PROPERTY_NOT_FOUND + prop);
    }

    private void checkSignature(AnnotationProperty prop) {
        if (!annotationProperties.contains(prop.getName()))
            throw new IllegalArgumentException(ANNOTATION_PROPERTY_NOT_FOUND + prop);
    }



    final static class Hierarchy<T extends DescriptionBT> {
        private final ImmutableList.Builder<BinaryAxiom<T>> inclusions = ImmutableList.builder();
        private final ImmutableList.Builder<NaryAxiom<T>> disjointness = ImmutableList.builder();

        /**
         * implements rules [D1], [O1] and [C1]:<br>
         *    - ignore if e1 is bot or e2 is top<br>
         *    - replace by emptiness if e2 is bot but e1 is not top<br>
         *    - inconsistency if e1 is top and e2 is bot
         *
         * @param e1
         * @param e2
         * @throws InconsistentOntologyException
         */

        void addInclusion(T e1, T e2) throws InconsistentOntologyException {
            if (e1.isBottom() || e2.isTop())
                return;

            if (e2.isBottom()) { // emptiness
                if (e1.isTop())
                    throw new InconsistentOntologyException();
                NaryAxiom<T> ax = new NaryAxiomImpl<>(ImmutableList.of(e1, e1));
                disjointness.add(ax);
            }
            else {
                BinaryAxiom<T> ax = new BinaryAxiomImpl<>(e1, e2);
                inclusions.add(ax);
            }
        }

        /**
         * implements an extension of [D2], [O2] and [C2]:<br>
         *     - eliminates all occurrences of bot and if the result contains<br>
         *     - no top and at least two elements then disjointness<br>
         *     - one top then emptiness of all other elements<br>
         *     - two tops then inconsistency (this behavior is an extension of OWL 2, where duplicates are removed from the list)
         *
         * @param es
         * @throws InconsistentOntologyException
         */

        void addDisjointness(T... es) throws InconsistentOntologyException {
            ImmutableList.Builder<T> sb = new ImmutableList.Builder<>();
            int numberOfTop = 0;
            for (T e : es) {
                //checkSignature(e);
                if (e.isBottom())
                    continue;
                else if (e.isTop())
                    numberOfTop++;
                else
                    sb.add(e);
            }
            ImmutableList<T> nonTrivialElements = sb.build();
            if (numberOfTop == 0) {
                if (nonTrivialElements.size() >= 2) {
                    NaryAxiomImpl<T> ax = new NaryAxiomImpl<>(nonTrivialElements);
                    disjointness.add(ax);
                }
                // if 0 or 1 non-bottom elements then do nothing
            }
            else if (numberOfTop == 1) {
                for (T dpe : nonTrivialElements) {
                    NaryAxiomImpl<T> ax = new NaryAxiomImpl<>(ImmutableList.of(dpe, dpe));
                    disjointness.add(ax);
                }
            }
            else // many tops
                throw new InconsistentOntologyException();
        }
    };
}
