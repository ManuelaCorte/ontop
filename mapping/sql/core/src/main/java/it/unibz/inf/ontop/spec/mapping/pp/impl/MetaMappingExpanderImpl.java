package it.unibz.inf.ontop.spec.mapping.pp.impl;

import com.google.common.collect.*;
import com.google.inject.Inject;
import it.unibz.inf.ontop.dbschema.DBParameters;
import it.unibz.inf.ontop.exception.MetaMappingExpansionException;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.injection.OntopSQLCredentialSettings;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.NativeNode;
import it.unibz.inf.ontop.iq.transform.IQTree2NativeNodeGenerator;
import it.unibz.inf.ontop.model.atom.RDFAtomPredicate;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.vocabulary.RDF;
import it.unibz.inf.ontop.spec.mapping.MappingAssertion;
import it.unibz.inf.ontop.iq.type.NotYetTypedEqualityTransformer;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import it.unibz.inf.ontop.utils.LocalJDBCConnectionUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class MetaMappingExpanderImpl implements MetaMappingExpander {

    private final SubstitutionFactory substitutionFactory;
    private final IntermediateQueryFactory iqFactory;
    private final TermFactory termFactory;
    private final NotYetTypedEqualityTransformer mappingEqualityTransformer;
    private final IQTree2NativeNodeGenerator nativeNodeGenerator;
    private final OntopSQLCredentialSettings settings;

    @Inject
    private MetaMappingExpanderImpl(SubstitutionFactory substitutionFactory,
                                    IntermediateQueryFactory iqFactory,
                                    TermFactory termFactory,
                                    NotYetTypedEqualityTransformer mappingEqualityTransformer,
                                    IQTree2NativeNodeGenerator nativeNodeGenerator,
                                    OntopSQLCredentialSettings settings) {
        this.substitutionFactory = substitutionFactory;
        this.iqFactory = iqFactory;
        this.termFactory = termFactory;
        this.mappingEqualityTransformer = mappingEqualityTransformer;
        this.nativeNodeGenerator = nativeNodeGenerator;
        this.settings = settings;
    }

    @Override
    public ImmutableList<MappingAssertion> transform(ImmutableList<MappingAssertion> mapping, DBParameters dbParameters)
            throws MetaMappingExpansionException {
        ImmutableList.Builder<MappingAssertion> resultBuilder = ImmutableList.builder();
        ImmutableList.Builder<ExpansionPosition> positionsBuilder = ImmutableList.builder();

        for (MappingAssertion assertion : mapping) {
            Optional<ExpansionPosition> position = getExpansionPosition(assertion);
            if (!position.isPresent())
                resultBuilder.add(assertion);
            else
                positionsBuilder.add(position.get());
        }

        ImmutableList<ExpansionPosition> positions = positionsBuilder.build();
        if (positions.isEmpty())
            return mapping;

        try (Connection connection = LocalJDBCConnectionUtils.createConnection(settings)) {
            for (ExpansionPosition position : positions) {
                NativeNode nativeNode = position.getDatabaseQuery(dbParameters);
                try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(nativeNode.getNativeQueryString())) {
                    while (rs.next()) {
                        ImmutableSubstitution<ImmutableTerm> sub = substitutionFactory.getSubstitutionThrowsExceptions(
                                nativeNode.getVariables(),
                                v -> v,
                                v -> termFactory.getDBConstant(
                                        rs.getString(nativeNode.getColumnNames().get(v).getName()),
                                        nativeNode.getTypeMap().get(v)));

                        resultBuilder.add(position.createExpansion(sub));
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new MetaMappingExpansionException(e);
        }

        return resultBuilder.build();
    }

    private final class ExpansionPosition {
        private final MappingAssertion assertion;
        private final Variable topVariable;

        ExpansionPosition(MappingAssertion assertion, Variable topVariable) {
            this.assertion = assertion;
            this.topVariable = topVariable;
        }

        ImmutableTerm getTemplate() {
            return assertion.getTopSubstitution().get(topVariable);
        }

        NativeNode getDatabaseQuery(DBParameters dbParameters) {

            IQTree topChildNotNull = termFactory.getDBIsNotNull(assertion.getTopChild().getVariables().stream())
                    .map(iqFactory::createFilterNode)
                    .map(n -> (IQTree)iqFactory.createUnaryIQTree(n, assertion.getTopChild()))
                    .orElse(assertion.getTopChild());

            IQTree constructionTree = iqFactory.createUnaryIQTree(iqFactory.createConstructionNode(
                    getTemplate().getVariableStream().collect(ImmutableCollectors.toSet()),
                    substitutionFactory.getSubstitution()),
                    topChildNotNull);

            IQTree tree = iqFactory.createUnaryIQTree(iqFactory.createDistinctNode(), constructionTree);

            IQTree transformedTree = mappingEqualityTransformer.transform(tree);
            return nativeNodeGenerator.generate(transformedTree, dbParameters, true);
        }

        MappingAssertion createExpansion(ImmutableSubstitution<ImmutableTerm> values) {
            ImmutableTerm instantiatedTemplate = values.apply(getTemplate());
            ImmutableSubstitution<ImmutableTerm> instantiatedSub = substitutionFactory.replace(assertion.getTopSubstitution(), topVariable, instantiatedTemplate);

            IQTree filterTree = iqFactory.createUnaryIQTree(iqFactory.createFilterNode(
                            termFactory.getConjunction(values.builder()
                                    .toStream(termFactory::getNotYetTypedEquality)
                                    .collect(ImmutableCollectors.toList()))),
                            assertion.getTopChild());

            IQTree tree = iqFactory.createUnaryIQTree(iqFactory.createConstructionNode(
                            instantiatedSub.getDomain(), instantiatedSub),
                            filterTree);

            return assertion.copyOf(tree, iqFactory);
        }

    }

    private Optional<ExpansionPosition> getExpansionPosition(MappingAssertion assertion) {
        RDFAtomPredicate predicate = assertion.getRDFAtomPredicate();

        RDFAtomPredicate.ComponentGetter componentGetter = predicate.getPropertyIRI(assertion.getTerms())
                        .filter(p -> p.equals(RDF.TYPE))
                        .isPresent()
                        ? predicate::getObject
                        : predicate::getProperty;

        return (componentGetter.get(assertion.getTerms()).isGround())
                ? Optional.empty()
                : Optional.of(new ExpansionPosition(assertion,
                        componentGetter.get(assertion.getProjectionAtom().getArguments())));
    }
}
