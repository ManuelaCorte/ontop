package it.unibz.inf.ontop.materialization.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.dbschema.RelationDefinition;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.ConstructionNode;
import it.unibz.inf.ontop.iq.node.ExtensionalDataNode;
import it.unibz.inf.ontop.materialization.RDFFactTemplates;
import it.unibz.inf.ontop.materialization.MappingAssertionInformation;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.substitution.Substitution;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import org.eclipse.rdf4j.model.IRI;

import java.util.Collection;
import java.util.Optional;

public class ComplexMappingAssertionInfo implements MappingAssertionInformation {
    private final IQTree tree;
    private final RDFFactTemplates rdfFactTemplates;

    public ComplexMappingAssertionInfo(IQTree tree, RDFFactTemplates rdfFactTemplates) {
        this.tree = tree;
        this.rdfFactTemplates = rdfFactTemplates;
    }

    @Override
    public Optional<MappingAssertionInformation> merge(MappingAssertionInformation other) {
        return Optional.empty();
    }

    @Override
    public IQTree getIQTree() {
        return tree;
    }

    @Override
    public RDFFactTemplates getRDFFactTemplates() {
        return rdfFactTemplates;
    }

    @Override
    public RDFFactTemplates restrict(ImmutableSet<IRI> predicates) {
        ImmutableCollection<ImmutableList<Variable>> filteredTemplates = rdfFactTemplates.getTriplesOrQuadsVariables().stream()
                .filter(tripleOrQuad -> {
                    Substitution<ImmutableTerm> topConstructSubstitution = ((ConstructionNode) tree.getRootNode()).getSubstitution();
                    ImmutableTerm predicate = topConstructSubstitution.apply(tripleOrQuad.get(1));
                    return predicate instanceof IRI && predicates.contains(predicate);
                })
                .collect(ImmutableCollectors.toList());

        return new RDFFactTemplatesImpl(filteredTemplates);
    }

    @Override
    public ImmutableList<RelationDefinition> getRelationsDefinitions() {
        return findRelations(tree);
    }

    private ImmutableList<RelationDefinition> findRelations(IQTree tree) {
        if (tree.getChildren().isEmpty()) {
            if (tree.getRootNode() instanceof ExtensionalDataNode) {
                return ImmutableList.of(((ExtensionalDataNode) tree.getRootNode()).getRelationDefinition());
            }
        } else {
            return tree.getChildren().stream()
                    .map(this::findRelations)
                    .flatMap(Collection::stream)
                    .collect(ImmutableCollectors.toList());
        }
        return ImmutableList.of();
    }
}
