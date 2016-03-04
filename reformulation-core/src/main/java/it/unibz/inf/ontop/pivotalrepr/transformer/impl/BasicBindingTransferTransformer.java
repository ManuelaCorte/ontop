package it.unibz.inf.ontop.pivotalrepr.transformer.impl;

import java.util.Optional;
import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.ImmutableExpression;
import it.unibz.inf.ontop.model.ImmutableTerm;
import it.unibz.inf.ontop.model.NonGroundTerm;
import it.unibz.inf.ontop.pivotalrepr.impl.FilterNodeImpl;
import it.unibz.inf.ontop.pivotalrepr.impl.GroupNodeImpl;
import it.unibz.inf.ontop.pivotalrepr.impl.InnerJoinNodeImpl;
import it.unibz.inf.ontop.pivotalrepr.impl.LeftJoinNodeImpl;
import it.unibz.inf.ontop.pivotalrepr.proposal.BindingTransfer;
import it.unibz.inf.ontop.pivotalrepr.transformer.BindingTransferTransformer;
import it.unibz.inf.ontop.model.ImmutableSubstitution;
import it.unibz.inf.ontop.pivotalrepr.*;

/**
 * Basic implementation: applies the bindings directly
 *
 * TODO: propose an optimized version that "extracts" the relevant variables from the bindings.
 *
 */
public class BasicBindingTransferTransformer implements BindingTransferTransformer {

    private final ImmutableSubstitution<ImmutableTerm> transferredBindings;

    public BasicBindingTransferTransformer(BindingTransfer transfer) {
        transferredBindings = transfer.getTransferredBindings();
    }

    @Override
    public FilterNode transform(FilterNode filterNode) {
        ImmutableExpression newBooleanExpression =
                transformOptionalFilterCondition(filterNode.getOptionalFilterCondition()).get();
        return new FilterNodeImpl(newBooleanExpression);
    }

    @Override
    public ExtensionalDataNode transform(ExtensionalDataNode extensionalDataNode) {
        return extensionalDataNode;
    }

    @Override
    public LeftJoinNode transform(LeftJoinNode leftJoinNode) {
        return new LeftJoinNodeImpl(transformOptionalFilterCondition(leftJoinNode.getOptionalFilterCondition()));
    }

    @Override
    public UnionNode transform(UnionNode unionNode) {
        return unionNode;
    }

    @Override
    public IntensionalDataNode transform(IntensionalDataNode intensionalDataNode) {
        return intensionalDataNode;
    }

    @Override
    public InnerJoinNode transform(InnerJoinNode innerJoinNode)  {
        return new InnerJoinNodeImpl(transformOptionalFilterCondition(innerJoinNode.getOptionalFilterCondition()));
    }

    @Override
    public ConstructionNode transform(ConstructionNode constructionNode) {
        return constructionNode;
    }

    @Override
    public GroupNode transform(GroupNode groupNode) throws NotNeededNodeException {
        ImmutableList.Builder<NonGroundTerm> groupingTermBuilder = ImmutableList.builder();
        for (NonGroundTerm groupingTerm : groupNode.getGroupingTerms()) {
            ImmutableTerm newTerm = transferredBindings.apply(groupingTerm);

            /**
             * We ignore the ground terms.
             */
            if (newTerm instanceof NonGroundTerm) {
                groupingTermBuilder.add((NonGroundTerm) newTerm);
            }
        }
        ImmutableList<NonGroundTerm> newGroupingTerms = groupingTermBuilder.build();
        /**
         * Declares the node as not needed anymore if it has no more grouping term.
         */
        if (newGroupingTerms.isEmpty()) {
            throw new NotNeededNodeException("This GROUP node is not needed anymore");
        }
        return new GroupNodeImpl(newGroupingTerms);
    }

    private Optional<ImmutableExpression> transformOptionalFilterCondition(
            Optional<ImmutableExpression> optionalFilterCondition) {
        if (optionalFilterCondition.isPresent()) {
            return Optional.of(transferredBindings.applyToBooleanExpression(optionalFilterCondition.get()));
        }
        else {
            return Optional.empty();
        }
    }
}
