package com.example.actions;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business process action template using AbstractSimpleDecisionAction.
 *
 * Returns OK or NOK transition based on the action's logic.
 * Register as a Spring bean with parent="abstractAction".
 *
 * For actions with more than two outcomes, extend AbstractAction instead
 * and override getTransitions() to declare custom transition names.
 */
public class CustomProcessAction extends AbstractSimpleDecisionAction<OrderProcessModel> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessAction.class);

    @Override
    public Transition executeAction(final OrderProcessModel process) {
        final OrderModel order = process.getOrder();

        if (order == null) {
            LOG.error("Process {} has no order attached", process.getCode());
            return Transition.NOK;
        }

        LOG.info("Processing order: {}", order.getCode());

        try {
            // TODO: Replace with actual business logic
            // Examples:
            //   - Validate order entries
            //   - Check payment authorization
            //   - Reserve inventory
            //   - Call external fulfillment API
            //   - Send notification emails

            final boolean success = performAction(order);

            if (success) {
                LOG.info("Action completed successfully for order: {}", order.getCode());
                return Transition.OK;
            } else {
                LOG.warn("Action failed for order: {}", order.getCode());
                return Transition.NOK;
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error processing order: {}", order.getCode(), e);
            return Transition.NOK;
        }
    }

    private boolean performAction(final OrderModel order) {
        // TODO: Implement action logic
        return true;
    }
}
