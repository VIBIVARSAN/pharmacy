<c:set var="pageTitle" value="Settings"/><c:set var="nav" value="settings"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Settings</h1><p class="lead-text">Pharmacy details and the rules used for billing and stock alerts.</p></div>
</div>
<c:if test="${empty values.tax_rate_percent}">
    <div class="alert alert-warning d-flex gap-2" role="status"><ui:icon name="exclamation-triangle" css="mt-1"/><div><strong>No tax rate is configured.</strong> Invoices cannot be created until you enter one below. Enter 0 if no tax applies.</div></div>
</c:if>
<ui:errors/>
<form method="post" action="${ctx}/settings" class="needs-validation" novalidate data-once>
    <ui:csrf/>
    <div class="card mb-4">
        <div class="card-header"><h2>Pharmacy details</h2><span class="text-muted small">Printed on invoices</span></div>
        <div class="card-body"><div class="row g-3">
            <ui:input name="s_pharmacy_name" label="Pharmacy name" value="${values.pharmacy_name}" required="true" maxlength="150"/>
            <ui:input name="s_pharmacy_phone" label="Phone" value="${values.pharmacy_phone}" maxlength="50" col="col-md-3"/>
            <ui:input name="s_pharmacy_license" label="Licence / registration number" value="${values.pharmacy_license}" maxlength="80" col="col-md-3"/>
            <ui:input name="s_pharmacy_address" label="Address" value="${values.pharmacy_address}" maxlength="300" col="col-12"/>
        </div></div>
    </div>
    <div class="card mb-4">
        <div class="card-header"><h2>Billing</h2></div>
        <div class="card-body"><div class="row g-3">
            <ui:input name="s_currency_symbol" label="Currency symbol" value="${values.currency_symbol}" required="true" maxlength="5" col="col-md-3"/>
            <ui:input name="s_tax_rate_percent" label="Tax rate (%)" type="number" value="${values.tax_rate_percent}" min="0" max="100" step="0.01" col="col-md-3" help="Applied to the amount after discounts. 0 = no tax."/>
            <div class="col-md-3">
                <label class="form-label" for="f_s_discounts_enabled">Line discounts</label>
                <select class="form-select" id="f_s_discounts_enabled" name="s_discounts_enabled">
                    <option value="false" ${values.discounts_enabled == 'true' ? '' : 'selected'}>Not allowed</option>
                    <option value="true" ${values.discounts_enabled == 'true' ? 'selected' : ''}>Allowed</option>
                </select>
                <div class="form-text">Lets staff enter a discount amount per invoice line.</div>
            </div>
            <ui:input name="s_payment_methods" label="Payment methods" value="${values.payment_methods}" required="true" maxlength="300" col="col-md-3" help="Comma separated, e.g. Cash,Card,UPI"/>
        </div></div>
    </div>
    <div class="card mb-4">
        <div class="card-header"><h2>Stock alerts</h2></div>
        <div class="card-body"><div class="row g-3">
            <ui:input name="s_near_expiry_days" label="Near-expiry window (days)" type="number" value="${values.near_expiry_days}" required="true" min="1" max="730" step="1" col="col-md-4" help="Batches expiring within this many days are flagged."/>
            <ui:input name="s_default_reorder_level" label="Default reorder level" type="number" value="${values.default_reorder_level}" required="true" min="0" step="1" col="col-md-4" help="Suggested when a new medicine is added. Each medicine keeps its own level."/>
        </div></div>
    </div>
    <div class="form-actions"><button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> Save settings</button></div>
</form>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
