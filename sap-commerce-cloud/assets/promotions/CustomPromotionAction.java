package com.example.promotions.actions;

import de.hybris.platform.ruleengineservices.rao.RuleEngineResultRAO;
import de.hybris.platform.ruleengineservices.rule.evaluation.RuleActionContext;
import de.hybris.platform.ruleengineservices.rule.evaluation.actions.AbstractRuleExecutableSupport;
import de.hybris.platform.ruleengineservices.rule.evaluation.actions.RAOAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Custom promotion action template.
 *
 * Implements a custom reward action for the promotion rule engine.
 * Register as a Spring bean with parent="abstractRuleExecutableSupport"
 * and reference in RuleActionDefinition ImpEx.
 *
 * This example awards loyalty points as a promotion benefit.
 * Adapt for other custom actions: gift wrapping, extended warranty, etc.
 */
public class CustomPromotionAction extends AbstractRuleExecutableSupport implements RAOAction {

    private static final Logger LOG = LoggerFactory.getLogger(CustomPromotionAction.class);

    @Override
    public boolean performActionInternal(final RuleActionContext context) {
        // Extract parameters defined in the RuleActionDefinition
        final BigDecimal points = context.getParameter("points", BigDecimal.class);

        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            LOG.warn("Invalid loyalty points value: {}", points);
            return false;
        }

        LOG.info("Awarding {} loyalty points", points);

        // TODO: Replace with actual custom action logic
        // Example: Create a custom RAO (Rule Action Object) to carry the action result
        //
        // final LoyaltyPointsRAO loyaltyRAO = new LoyaltyPointsRAO();
        // loyaltyRAO.setPoints(points.intValue());
        //
        // final RuleEngineResultRAO result = context.getRuleEngineResultRAO();
        // result.getActions().add(loyaltyRAO);
        //
        // context.scheduleForUpdate(result);
        // context.insertFacts(loyaltyRAO);

        return true;
    }
}
