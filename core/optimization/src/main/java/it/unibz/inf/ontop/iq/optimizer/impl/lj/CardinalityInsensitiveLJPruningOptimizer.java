package it.unibz.inf.ontop.iq.optimizer.impl.lj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unibz.inf.ontop.injection.CoreSingletons;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.*;
import it.unibz.inf.ontop.iq.optimizer.LeftJoinIQOptimizer;
import it.unibz.inf.ontop.iq.optimizer.impl.LookForDistinctOrLimit1TransformerImpl;
import it.unibz.inf.ontop.iq.transform.IQTreeTransformer;
import it.unibz.inf.ontop.iq.transform.IQTreeVisitingTransformer;
import it.unibz.inf.ontop.iq.transform.impl.DefaultNonRecursiveIQTreeTransformer;
import it.unibz.inf.ontop.model.term.DBConstant;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Prunes right children when their variables are not used outside the LJ
 * in a cardinality-insensitive context
 */
@Singleton
public class CardinalityInsensitiveLJPruningOptimizer implements LeftJoinIQOptimizer {
    private final CoreSingletons coreSingletons;
    private final IntermediateQueryFactory iqFactory;

    @Inject
    protected CardinalityInsensitiveLJPruningOptimizer(CoreSingletons coreSingletons) {
        this.coreSingletons = coreSingletons;
        this.iqFactory = coreSingletons.getIQFactory();
    }

    @Override
    public IQ optimize(IQ query) {
        IQTree initialTree = query.getTree();

        IQTreeVisitingTransformer transformer = new LookForDistinctOrLimit1TransformerImpl(
                (childTree, parentTransformer) -> new CardinalityInsensitiveLJPruningTransformer(
                        parentTransformer,
                        coreSingletons,
                        childTree.getVariables()),
                coreSingletons);

        IQTree newTree = initialTree.acceptTransformer(transformer);

        return newTree.equals(initialTree)
                ? query
                : iqFactory.createIQ(query.getProjectionAtom(), newTree);
    }

    protected static class CardinalityInsensitiveLJPruningTransformer extends DefaultNonRecursiveIQTreeTransformer {

        private final IQTreeTransformer lookForDistinctTransformer;
        private final CoreSingletons coreSingletons;
        private final ImmutableSet<Variable> variablesUsedByAncestors;
        private final IntermediateQueryFactory iqFactory;
        private final TermFactory termFactory;
        private final DBConstant provConstant;
        private final SubstitutionFactory substitutionFactory;

        protected CardinalityInsensitiveLJPruningTransformer(IQTreeTransformer lookForDistinctTransformer,
                                                             CoreSingletons coreSingletons,
                                                             ImmutableSet<Variable> variablesUsedByAncestors) {
            this.lookForDistinctTransformer = lookForDistinctTransformer;
            this.coreSingletons = coreSingletons;
            this.variablesUsedByAncestors = variablesUsedByAncestors;
            this.iqFactory = coreSingletons.getIQFactory();
            this.termFactory = coreSingletons.getTermFactory();
            this.substitutionFactory = coreSingletons.getSubstitutionFactory();
            this.provConstant = termFactory.getProvenanceSpecialConstant();
        }

        @Override
        public IQTree transformConstruction(IQTree tree, ConstructionNode rootNode, IQTree child) {
            var newVariablesUsed = Sets.union(variablesUsedByAncestors, rootNode.getLocallyRequiredVariables());

            var newTransformer = newVariablesUsed.equals(variablesUsedByAncestors)
                    ? this
                    : computeNewTransformer(newVariablesUsed.immutableCopy());

            IQTree newChild = child.acceptTransformer(newTransformer);
            return newChild.equals(child) && rootNode.equals(tree.getRootNode())
                    ? tree
                    : iqFactory.createUnaryIQTree(rootNode, newChild);
        }

        @Override
        public IQTree transformFilter(IQTree tree, FilterNode rootNode, IQTree child) {
            var newTransformer = rootNode.getOptionalFilterCondition()
                    .map(ImmutableFunctionalTerm::getVariables)
                    .filter(vs -> !vs.isEmpty())
                    .map(vs -> Sets.union(variablesUsedByAncestors, vs).immutableCopy())
                    .map(this::computeNewTransformer)
                    .orElse(this);

            IQTree newChild = child.acceptTransformer(newTransformer);
            return newChild.equals(child) && rootNode.equals(tree.getRootNode())
                    ? tree
                    : iqFactory.createUnaryIQTree(rootNode, newChild);
        }

        @Override
        public IQTree transformOrderBy(IQTree tree, OrderByNode rootNode, IQTree child) {
            return applyRecursivelyToUnaryNode(tree, rootNode, child);
        }

        protected IQTree applyRecursivelyToUnaryNode(IQTree tree, UnaryOperatorNode rootNode, IQTree child) {
            IQTree newChild = child.acceptTransformer(this);
            return newChild.equals(child) && rootNode.equals(tree.getRootNode())
                    ? tree
                    : iqFactory.createUnaryIQTree(rootNode, newChild);
        }

        @Override
        public IQTree transformLeftJoin(IQTree tree, LeftJoinNode rootNode, IQTree leftChild, IQTree rightChild) {
            var treeVariables = tree.getVariables();

            var leftVariables = leftChild.getVariables();

            var usedVariables = Sets.intersection(variablesUsedByAncestors, treeVariables);

            if (treeVariables.isEmpty() || leftVariables.containsAll(usedVariables))
                // Prunes the right child
                return leftChild.acceptTransformer(this);

            var provVariables = extractProvenanceVariables(rightChild);
            if ((!provVariables.isEmpty()) && leftVariables.containsAll(Sets.difference(usedVariables, provVariables)))
                return liftProvenanceAndPruneRightChild(leftChild, rightChild, provVariables, treeVariables);


            var commonVariables = Sets.intersection(leftVariables, rightChild.getVariables());

            var newVariablesUsed = rootNode.getOptionalFilterCondition()
                    .map(ImmutableFunctionalTerm::getVariables)
                    .filter(vs -> !vs.isEmpty())
                    .map(vs -> Sets.union(variablesUsedByAncestors, Sets.union(vs, commonVariables)).immutableCopy())
                    .orElse(Sets.union(variablesUsedByAncestors, commonVariables).immutableCopy());

            var newTransformer = newVariablesUsed.equals(variablesUsedByAncestors)
                    ? this
                    : computeNewTransformer(newVariablesUsed);

            var newLeft = leftChild.acceptTransformer(newTransformer);
            var newRight = rightChild.acceptTransformer(newTransformer);

            return newLeft.equals(leftChild) && newRight.equals(rightChild)
                    ? tree
                    : iqFactory.createBinaryNonCommutativeIQTree(rootNode, newLeft, newRight);
        }

        private IQTree liftProvenanceAndPruneRightChild(IQTree leftChild, IQTree rightChild,
                                                        ImmutableSet<Variable> provVariables,
                                                        ImmutableSet<Variable> treeVariables) {
            var leftVariables = leftChild.getVariables();
            var commonVariables = Sets.intersection(leftVariables, rightChild.getVariables());
            var commonProvVariables = Sets.intersection(commonVariables, provVariables);

            var condition = termFactory.getConjunction(Stream.concat(
                    commonVariables.stream()
                            .map(termFactory::getDBIsNotNull),
                    // Should not happen in practice, but just in case
                    commonProvVariables.stream()
                            .map(v -> termFactory.getStrictEquality(v, provConstant)
                    )));

            var constructionNode = condition
                    .map(c -> provVariables.stream().collect(
                            substitutionFactory.toSubstitution(v -> termFactory.getIfElseNull(c, provConstant))))
                    .map(s -> iqFactory.createConstructionNode(treeVariables, s));

            var newLeftChild = leftChild.acceptTransformer(this);

            return constructionNode
                    .map(n -> (IQTree) iqFactory.createUnaryIQTree(n, newLeftChild))
                    .orElse(newLeftChild);
        }

        private ImmutableSet<Variable> extractProvenanceVariables(IQTree rightChild) {
            var rootNode = rightChild.getRootNode();
            if (rootNode instanceof ConstructionNode) {
                var substitution = ((ConstructionNode) rootNode).getSubstitution();
                return substitution.stream()
                        .filter(e -> e.getValue().equals(provConstant))
                        .map(Map.Entry::getKey)
                        .collect(ImmutableSet.toImmutableSet());
            }
            else
                return ImmutableSet.of();
        }

        @Override
        public IQTree transformUnion(IQTree tree, UnionNode rootNode, ImmutableList<IQTree> children) {
            return applyRecursivelyToNaryNode(tree, rootNode, children);
        }

        @Override
        public IQTree transformInnerJoin(IQTree tree, InnerJoinNode rootNode, ImmutableList<IQTree> children) {
            var newTransformer = rootNode.getOptionalFilterCondition()
                    .map(ImmutableFunctionalTerm::getVariables)
                    .filter(vs -> !vs.isEmpty())
                    .map(vs -> Sets.union(variablesUsedByAncestors, vs).immutableCopy())
                    .map(this::computeNewTransformer)
                    .orElse(this);

            ImmutableList<IQTree> newChildren = children.stream()
                    .map(t -> t.acceptTransformer(newTransformer))
                    .collect(ImmutableCollectors.toList());

            return newChildren.equals(children) && rootNode.equals(tree.getRootNode())
                    ? tree
                    : iqFactory.createNaryIQTree(rootNode, newChildren);
        }

        protected IQTree applyRecursivelyToNaryNode(IQTree tree, NaryOperatorNode rootNode, ImmutableList<IQTree> children) {
            ImmutableList<IQTree> newChildren = children.stream()
                    .map(t -> t.acceptTransformer(this))
                    .collect(ImmutableCollectors.toList());

            return newChildren.equals(children) && rootNode.equals(tree.getRootNode())
                    ? tree
                    : iqFactory.createNaryIQTree(rootNode, newChildren);
        }

        /**
         * Default behavior
         */
        @Override
        protected IQTree transformUnaryNode(IQTree tree, UnaryOperatorNode rootNode, IQTree child) {
            return lookForDistinctTransformer.transform(tree);
        }

        /**
         * Default behavior
         */
        @Override
        protected IQTree transformNaryCommutativeNode(IQTree tree, NaryOperatorNode rootNode, ImmutableList<IQTree> children) {
            return lookForDistinctTransformer.transform(tree);
        }

        /**
         * Default behavior
         */
        @Override
        protected IQTree transformBinaryNonCommutativeNode(IQTree tree, BinaryNonCommutativeOperatorNode rootNode, IQTree leftChild, IQTree rightChild) {
            return lookForDistinctTransformer.transform(tree);
        }

        private CardinalityInsensitiveLJPruningTransformer computeNewTransformer(ImmutableSet<Variable> newVariablesUsedByAncestors) {
            return new CardinalityInsensitiveLJPruningTransformer(lookForDistinctTransformer, coreSingletons, newVariablesUsedByAncestors);
        }
    }


}
