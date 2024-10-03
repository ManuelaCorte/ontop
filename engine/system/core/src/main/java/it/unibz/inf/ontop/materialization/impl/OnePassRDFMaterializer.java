package it.unibz.inf.ontop.materialization.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import it.unibz.inf.ontop.answering.OntopQueryEngine;
import it.unibz.inf.ontop.answering.logging.QueryLogger;
import it.unibz.inf.ontop.answering.reformulation.generation.NativeQueryGenerator;
import it.unibz.inf.ontop.answering.resultset.MaterializedGraphResultSet;
import it.unibz.inf.ontop.dbschema.RelationDefinition;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.exception.OBDASpecificationException;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.injection.OntopSystemConfiguration;
import it.unibz.inf.ontop.injection.OntopSystemFactory;
import it.unibz.inf.ontop.injection.TranslationFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.LeafIQTree;
import it.unibz.inf.ontop.iq.node.*;
import it.unibz.inf.ontop.iq.optimizer.GeneralStructuralAndSemanticIQOptimizer;
import it.unibz.inf.ontop.iq.planner.QueryPlanner;
import it.unibz.inf.ontop.materialization.MappingAssertionInformation;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.materialization.OntopRDFMaterializer;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.atom.QuadPredicate;
import it.unibz.inf.ontop.model.atom.RDFAtomPredicate;
import it.unibz.inf.ontop.model.atom.TriplePredicate;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.spec.OBDASpecification;
import it.unibz.inf.ontop.spec.mapping.Mapping;
import it.unibz.inf.ontop.substitution.Substitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import org.apache.commons.rdf.api.IRI;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnePassRDFMaterializer implements OntopRDFMaterializer {

    private final MaterializationParams params;
    private final OntopQueryEngine queryEngine;
    private final IntermediateQueryFactory iqFactory;
    private final SubstitutionFactory substitutionFactory;
    private final NativeQueryGenerator nativeQueryGenerator;
    private final AtomFactory atomFactory;
    private final GeneralStructuralAndSemanticIQOptimizer iqOptimizer;
    private final QueryPlanner queryPlanner;
    private final QueryLogger queryLogger;

    private final ImmutableMap<IRI, VocabularyEntry> vocabulary;
    private final ImmutableList<MappingAssertionInformation> mappingInformation;

    public OnePassRDFMaterializer(OntopSystemConfiguration configuration, MaterializationParams materializationParams) throws OBDASpecificationException {
        Injector injector = configuration.getInjector();
        OntopSystemFactory engineFactory = injector.getInstance(OntopSystemFactory.class);

        OBDASpecification specification = configuration.loadSpecification();
        this.queryEngine = engineFactory.create(specification);
        this.vocabulary = extractVocabulary(specification.getSaturatedMapping());
        this.params = materializationParams;
        this.iqFactory = injector.getInstance(IntermediateQueryFactory.class);
        this.substitutionFactory = injector.getInstance(SubstitutionFactory.class);
        this.nativeQueryGenerator = injector.getInstance(TranslationFactory.class).create(specification.getDBParameters());
        this.atomFactory = injector.getInstance(AtomFactory.class);
        this.iqOptimizer = injector.getInstance(GeneralStructuralAndSemanticIQOptimizer.class);
        this.queryPlanner = injector.getInstance(QueryPlanner.class);
        QueryLogger.Factory queryLoggerFactory = injector.getInstance(QueryLogger.Factory.class);
        this.queryLogger = queryLoggerFactory.create(ImmutableMap.of());

        Mapping saturatedMapping = specification.getSaturatedMapping();
        ImmutableList<IQ> mappingAssertionsIQs = saturatedMapping.getRDFAtomPredicates().stream()
                .map(saturatedMapping::getQueries)
                .flatMap(Collection::stream)
                .collect(ImmutableCollectors.toList());

        mappingInformation = mergeMappingInformation(mappingAssertionsIQs.stream()
                .map(this::getMappingAssertionInfo)
                .collect(ImmutableCollectors.toList()));
    }

    @Override
    public MaterializedGraphResultSet materialize() {
        return new OnePassMaterializedGraphResultSet(vocabulary,
                mappingInformation,
                params,
                queryEngine,
                nativeQueryGenerator,
                atomFactory,
                iqFactory,
                iqOptimizer,
                queryPlanner,
                queryLogger);
    }

    @Override
    public MaterializedGraphResultSet materialize(@Nonnull ImmutableSet<IRI> selectedVocabulary) {
        throw new UnsupportedOperationException("To materialize different classes/properties in separate files, use the default materializer");
    }

    @Override
    public ImmutableSet<IRI> getClasses() {
        return vocabulary.entrySet().stream()
                .filter(e -> e.getValue().arity == 1)
                .map(Map.Entry::getKey)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public ImmutableSet<IRI> getProperties() {
        return vocabulary.entrySet().stream()
                .filter(e -> e.getValue().arity == 2)
                .map(Map.Entry::getKey)
                .collect(ImmutableCollectors.toSet());
    }

    private static ImmutableMap<IRI, VocabularyEntry> extractVocabulary(@Nonnull Mapping mapping) {
        Map<IRI, VocabularyEntry> result = new HashMap<>();
        for (RDFAtomPredicate predicate : mapping.getRDFAtomPredicates()) {
            if (predicate instanceof TriplePredicate || predicate instanceof QuadPredicate)
                result.putAll(extractTripleVocabulary(mapping, predicate)
                        .collect(ImmutableCollectors.toMap(e -> e.name, e -> e)));
        }
        return ImmutableMap.copyOf(result);
    }

    private static Stream<VocabularyEntry> extractTripleVocabulary(Mapping mapping, RDFAtomPredicate tripleOrQuadPredicate) {
        Stream<VocabularyEntry> vocabularyPropertyStream = mapping.getRDFProperties(tripleOrQuadPredicate).stream()
                .map(p -> new VocabularyEntry(p, 2));

        Stream<VocabularyEntry> vocabularyClassStream = mapping.getRDFClasses(tripleOrQuadPredicate).stream()
                .map(p -> new VocabularyEntry(p, 1));
        return Stream.concat(vocabularyClassStream, vocabularyPropertyStream);
    }

    private ImmutableMap<IRI, VocabularyEntry> filterVocabularyEntries(ImmutableSet<IRI> selectedVocabulary) {
        return vocabulary.entrySet().stream()
                .filter(e -> selectedVocabulary.contains(e.getKey()))
                .collect(ImmutableCollectors.toMap());
    }

    private ImmutableList<MappingAssertionInformation> mergeMappingInformation(ImmutableList<MappingAssertionInformation> mappingInformation) {
        ImmutableList<MappingAssertionInformation> complexMappingAssertionInfo = mappingInformation.stream()
                .filter(m -> m instanceof ComplexMappingAssertionInfo)
                .collect(ImmutableCollectors.toList());

        ImmutableList<SimpleMappingAssertionInfo> simpleMappingAssertionInfo = mappingInformation.stream()
                .filter(m -> m instanceof SimpleMappingAssertionInfo)
                .map(m -> (SimpleMappingAssertionInfo) m)
                .collect(ImmutableCollectors.toList());

        ImmutableMap<String, ImmutableList<SimpleMappingAssertionInfo>> groupedByRelationMappingsInfo = simpleMappingAssertionInfo.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(SimpleMappingAssertionInfo::getRelationName, ImmutableCollectors.toList()),
                        ImmutableMap::copyOf
                ));

        ImmutableList<MappingAssertionInformation> mergedSimpleMappingsInfo = groupedByRelationMappingsInfo.values().stream()
                .map(mappingInfos -> {
                    SimpleMappingAssertionInfo firstMappingInfo = mappingInfos.get(0);
                    return mappingInfos.stream()
                            .skip(1)
                            .reduce(firstMappingInfo, (m1, m2) -> (SimpleMappingAssertionInfo) m1.merge(m2).orElseThrow());
                })
                .collect(ImmutableCollectors.toList());

        return ImmutableList.<MappingAssertionInformation>builder()
                .addAll(mergedSimpleMappingsInfo)
                .addAll(complexMappingAssertionInfo)
                .build();
    }

    private MappingAssertionInformation getMappingAssertionInfo(IQ mappingAssertionIQ) {
        IQTree tree = mappingAssertionIQ.getTree();
        if (!(tree.getRootNode() instanceof ConstructionNode)) {
            throw new MinorOntopInternalBugException("The root node of a mapping is expected to be a ConstructionNode");
        }
        Substitution<ImmutableTerm> topSubstitution = ((ConstructionNode) tree.getRootNode()).getSubstitution();

        ImmutableList<LeafIQTree> leaves = findLeaves(tree);
        ImmutableList<ExtensionalDataNode> extensionalNodes = extractExtensionalNodes(leaves);
        ImmutableList<ImmutableMap<Variable, Constant>> valuesNodes = extractValuesNodes(leaves).stream()
                .map(ValuesNode::getValueMaps)
                .flatMap(Collection::stream)
                .collect(ImmutableCollectors.toList());

        ImmutableList<RelationDefinition> relationalSources = extensionalNodes.stream()
                .map(ExtensionalDataNode::getRelationDefinition)
                .collect(ImmutableCollectors.toList());

        ImmutableMap<Integer, ArrayList<VariableOrGroundTerm>> columnsMap = extensionalNodes.stream()
                .map(ExtensionalDataNode::getArgumentMap)
                .map(ImmutableMap::entrySet)
                .flatMap(Collection::stream)
                .collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(Collections.singleton(e.getValue())),
                        (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        }
                ));
        ImmutableMap<Integer, ImmutableList<VariableOrGroundTerm>> immutableColumnsMap = columnsMap.entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().distinct().collect(ImmutableCollectors.toList())
                ));

        if (relationalSources.size() == 1 &&
                valuesNodes.isEmpty() &&
                !hasFilterNode(tree) &&
                !hasDistinctNode(tree) &&
                immutableColumnsMap.values().stream().allMatch(l -> l.size() == 1)) {
            ImmutableMap<Integer, VariableOrGroundTerm> argumentMap = immutableColumnsMap.entrySet().stream()
                    .collect(ImmutableCollectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().get(0)
                    ));
            if (argumentMap.values().stream().allMatch(v -> v instanceof Variable)) {
                return new SimpleMappingAssertionInfo(
                        relationalSources.get(0),
                        argumentMap.entrySet().stream()
                                .collect(ImmutableCollectors.toMap(
                                        Map.Entry::getKey,
                                        e -> (Variable) e.getValue())),
                        topSubstitution,
                        tree,
                        new RDFFactTemplatesImpl(ImmutableList.of((mappingAssertionIQ.getProjectionAtom().getArguments()))),
                        mappingAssertionIQ.getVariableGenerator(),
                        iqFactory,
                        substitutionFactory);
            } else {
                return new ComplexMappingAssertionInfo(mappingAssertionIQ);
            }
        } else {
            return new ComplexMappingAssertionInfo(mappingAssertionIQ);
        }
    }

    private ImmutableList<ExtensionalDataNode> extractExtensionalNodes(ImmutableList<LeafIQTree> leaves) {
        return leaves.stream()
                .filter(node -> node instanceof ExtensionalDataNode)
                .map(node -> (ExtensionalDataNode) node)
                .collect(ImmutableCollectors.toList());
    }

    private ImmutableList<ValuesNode> extractValuesNodes(ImmutableList<LeafIQTree> leaves) {
        return leaves.stream()
                .filter(node -> node instanceof ValuesNode)
                .map(node -> (ValuesNode) node)
                .collect(ImmutableCollectors.toList());
    }

    /**
     * Recursive
     */
    private ImmutableList<LeafIQTree> findLeaves(IQTree tree) {
        if (tree.getChildren().isEmpty()) {
            if (tree.getRootNode() instanceof ExtensionalDataNode || tree.getRootNode() instanceof ValuesNode) {
                return ImmutableList.of((LeafIQTree) tree.getRootNode());
            } else {
                throw new MinorOntopInternalBugException("The leaf node of a mapping assertion is expected to be an ExtensionalDataNode or a ValuesNode");
            }
        } else {
            return tree.getChildren().stream()
                    .map(this::findLeaves)
                    .flatMap(Collection::stream)
                    .collect(ImmutableCollectors.toList());
        }
    }

    /**
     * Recursive
     */
    private boolean hasFilterNode(IQTree tree) {
        if (tree.getRootNode() instanceof JoinOrFilterNode) {
            return true;
        } else {
            return tree.getChildren().stream()
                    .anyMatch(this::hasFilterNode);
        }
    }

    /**
     * Recursive
     */
    private boolean hasDistinctNode(IQTree tree) {
        if (tree.getRootNode() instanceof DistinctNode) {
            return true;
        } else {
            return tree.getChildren().stream()
                    .anyMatch(this::hasDistinctNode);
        }
    }
}
