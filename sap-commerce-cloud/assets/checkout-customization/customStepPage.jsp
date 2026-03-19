<%-- customStepPage.jsp - Custom checkout step JSP page --%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags" %>
<%@ taglib prefix="multi-checkout" tagdir="/WEB-INF/tags/responsive/checkout/multi" %>

<spring:htmlEscape defaultHtmlEscape="true"/>

<template:page pageTitle="${pageTitle}">

    <div class="row">
        <div class="col-sm-6">
            <div class="checkout-headline">
                <spring:theme code="checkout.multi.customStep.title" text="Custom Options"/>
            </div>

            <%-- Error messages --%>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger">
                    <spring:theme code="${errorMessage}"/>
                </div>
            </c:if>

            <%-- Custom step form --%>
            <form:form id="customStepForm"
                       action="${request.contextPath}/checkout/multi/custom-step"
                       method="post"
                       modelAttribute="customStepForm">

                <div class="form-group">
                    <label for="customOption">
                        <spring:theme code="checkout.multi.customStep.selectOption" text="Select Option"/>
                    </label>

                    <form:select path="customOption" id="customOption" class="form-control">
                        <form:option value="">
                            <spring:theme code="checkout.multi.customStep.selectOption.placeholder"/>
                        </form:option>
                        <c:forEach items="${customOptions}" var="option">
                            <form:option value="${option.code}">
                                ${option.name}
                            </form:option>
                        </c:forEach>
                    </form:select>

                    <form:errors path="customOption" cssClass="help-block text-danger"/>
                </div>

                <div class="form-group">
                    <label for="customNotes">
                        <spring:theme code="checkout.multi.customStep.notes" text="Additional Notes"/>
                    </label>
                    <form:textarea path="customNotes" id="customNotes"
                                   class="form-control" rows="3"
                                   placeholder="Optional notes..."/>
                </div>

                <%-- Navigation buttons --%>
                <div class="checkout-steps-actions">
                    <a href="${request.contextPath}/checkout/multi/custom-step/back"
                       class="btn btn-default">
                        <spring:theme code="checkout.multi.back" text="Back"/>
                    </a>

                    <button type="submit" class="btn btn-primary">
                        <spring:theme code="checkout.multi.next" text="Next"/>
                    </button>
                </div>

            </form:form>
        </div>

        <%-- Order summary sidebar --%>
        <div class="col-sm-6">
            <multi-checkout:checkoutOrderSummary cartData="${cartData}"
                                                  showDeliveryAddress="true"
                                                  showDeliveryMethod="true"/>
        </div>
    </div>

    <%-- Progress indicator --%>
    <multi-checkout:checkoutProgressBar steps="${checkoutSteps}"
                                         currentStep="customStep"/>

</template:page>
