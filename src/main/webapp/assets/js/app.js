/*
 * Pharmacy Prescription Management System - shared page behaviour.
 * No inline scripts are used anywhere (the Content-Security-Policy forbids
 * them); pages opt in to behaviour through data-* attributes.
 */
(function () {
    'use strict';

    var ctx = document.body.getAttribute('data-ctx') || '';

    function $(selector, root) { return (root || document).querySelector(selector); }
    function $all(selector, root) { return Array.prototype.slice.call((root || document).querySelectorAll(selector)); }

    function icon(name) {
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('class', 'icon');
        svg.setAttribute('aria-hidden', 'true');
        var use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        use.setAttribute('href', ctx + '/assets/img/icons.svg#' + name);
        svg.appendChild(use);
        return svg;
    }

    /** Shows a toast created in the browser (server messages are rendered by the JSP). */
    function showToast(message, type) {
        var area = $('#toastArea');
        if (!area || !window.bootstrap) { return; }
        var toast = document.createElement('div');
        toast.className = 'toast app-toast toast-' + (type || 'info');
        toast.setAttribute('role', type === 'danger' ? 'alert' : 'status');
        var row = document.createElement('div');
        row.className = 'd-flex';
        var body = document.createElement('div');
        body.className = 'toast-body';
        body.appendChild(icon(type === 'danger' ? 'x-circle' : type === 'success' ? 'check-circle' : 'info-circle'));
        var text = document.createElement('span');
        text.textContent = message;
        body.appendChild(text);
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'btn-close me-2 m-auto';
        close.setAttribute('data-bs-dismiss', 'toast');
        close.setAttribute('aria-label', 'Close');
        row.appendChild(body);
        row.appendChild(close);
        toast.appendChild(row);
        area.appendChild(toast);
        new bootstrap.Toast(toast, { delay: type === 'danger' ? 9000 : 5000 }).show();
    }
    window.pmsToast = showToast;

    /** GET a JSON endpoint; rejects with a readable message on any failure. */
    function getJson(path) {
        return fetch(ctx + path, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
            .then(function (response) {
                return response.json().catch(function () { return {}; }).then(function (data) {
                    if (!response.ok) {
                        throw new Error(data.error || 'The server could not complete the request.');
                    }
                    return data;
                });
            });
    }
    window.pmsGetJson = getJson;

    // ------------------------------------------------------------ sidebar
    function initSidebar() {
        var toggle = $('#sidebarToggle');
        if (!toggle) { return; }
        var desktop = window.matchMedia('(min-width: 992px)');
        try {
            if (desktop.matches && localStorage.getItem('pms.sidebar') === 'collapsed') {
                document.body.classList.add('sidebar-collapsed');
            }
        } catch (e) { /* storage unavailable: keep default */ }
        function sync() {
            var open = desktop.matches ? !document.body.classList.contains('sidebar-collapsed')
                : document.body.classList.contains('sidebar-open');
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        }
        toggle.addEventListener('click', function () {
            if (desktop.matches) {
                var collapsed = document.body.classList.toggle('sidebar-collapsed');
                try { localStorage.setItem('pms.sidebar', collapsed ? 'collapsed' : 'open'); } catch (e) { /* ignore */ }
            } else {
                document.body.classList.toggle('sidebar-open');
            }
            sync();
        });
        $all('[data-sidebar-close]').forEach(function (el) {
            el.addEventListener('click', function () { document.body.classList.remove('sidebar-open'); sync(); });
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') { document.body.classList.remove('sidebar-open'); sync(); }
        });
        sync();
    }

    // ------------------------------------------------------------ toasts from the server
    function initToasts() {
        if (!window.bootstrap) { return; }
        $all('.toast[data-autoshow]').forEach(function (el) { new bootstrap.Toast(el).show(); });
    }

    // ------------------------------------------------------------ forms
    function initForms() {
        var confirmModalEl = $('#confirmModal');
        var confirmModal = confirmModalEl && window.bootstrap ? new bootstrap.Modal(confirmModalEl) : null;
        var pendingForm = null;

        function lock(form) {
            if (!form.hasAttribute('data-once')) { return; }
            $all('button[type="submit"]', form).forEach(function (button) {
                // keep the clicked button's name/value in the submitted data, then disable to stop double clicks
                window.setTimeout(function () { button.disabled = true; }, 0);
                if (!button.querySelector('.spinner-border')) {
                    var spinner = document.createElement('span');
                    spinner.className = 'spinner-border spinner-border-sm';
                    spinner.setAttribute('aria-hidden', 'true');
                    button.insertBefore(spinner, button.firstChild);
                }
            });
        }

        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement)) { return; }

            // 1. browser-side validation (the server validates again)
            if (form.classList.contains('needs-validation') && !form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                form.classList.add('was-validated');
                var firstInvalid = form.querySelector(':invalid');
                if (firstInvalid) { firstInvalid.focus(); }
                return;
            }

            // 2. confirmation dialog for destructive / irreversible actions
            var submitter = event.submitter;
            var message = (submitter && submitter.getAttribute('data-confirm')) || form.getAttribute('data-confirm');
            if (message && form.getAttribute('data-confirmed') !== 'yes') {
                event.preventDefault();
                if (!confirmModal) { return; }
                pendingForm = { form: form, submitter: submitter };
                $('#confirmMessage').textContent = message;
                var ok = $('#confirmOk');
                ok.textContent = (submitter && submitter.getAttribute('data-confirm-label')) || form.getAttribute('data-confirm-label') || 'Confirm';
                ok.className = 'btn ' + (((submitter && submitter.getAttribute('data-confirm-style')) || form.getAttribute('data-confirm-style')) === 'danger' ? 'btn-danger' : 'btn-primary');
                confirmModal.show();
                return;
            }
            lock(form);
        });

        var okButton = $('#confirmOk');
        if (okButton) {
            okButton.addEventListener('click', function () {
                if (!pendingForm) { return; }
                var p = pendingForm;
                pendingForm = null;
                p.form.setAttribute('data-confirmed', 'yes');
                confirmModal.hide();
                if (p.form.requestSubmit) {
                    p.form.requestSubmit(p.submitter && p.submitter.form === p.form ? p.submitter : undefined);
                } else {
                    p.form.submit();
                }
            });
        }

        // filters that apply as soon as they change
        $all('[data-autosubmit]').forEach(function (el) {
            el.addEventListener('change', function () { if (el.form) { el.form.submit(); } });
        });

        // print buttons
        $all('[data-print]').forEach(function (el) {
            el.addEventListener('click', function () { window.print(); });
        });
    }

    // ------------------------------------------------------------ modals filled from the trigger button
    // <button data-bs-toggle="modal" data-bs-target="#m" data-fill-batch-id="7" data-fill-label="B-01">
    // sets [data-field="batchId"] and [data-field="label"] inside the modal.
    function initModalFill() {
        document.addEventListener('show.bs.modal', function (event) {
            var trigger = event.relatedTarget;
            if (!trigger) { return; }
            var modal = event.target;
            var form = $('form', modal);
            if (form) { form.classList.remove('was-validated'); }
            Object.keys(trigger.dataset).forEach(function (key) {
                if (key.indexOf('fill') !== 0 || key.length <= 4) { return; }
                var field = key.charAt(4).toLowerCase() + key.slice(5);
                $all('[data-field="' + field + '"]', modal).forEach(function (target) {
                    if ('value' in target && target.tagName !== 'BUTTON') {
                        target.value = trigger.dataset[key];
                    } else {
                        target.textContent = trigger.dataset[key];
                    }
                });
                $all('[data-max-field="' + field + '"]', modal).forEach(function (target) {
                    target.max = trigger.dataset[key];
                });
            });
        });
        document.addEventListener('shown.bs.modal', function (event) {
            var first = $('[data-autofocus]', event.target);
            if (first) { first.focus(); }
        });
    }

    // ------------------------------------------------------------ stock alerts in the top bar
    function initAlerts() {
        var body = $('#alertsBody');
        if (!body) { return; }
        function row(cls, iconName, href, title, detail) {
            var a = document.createElement('a');
            a.className = 'alert-row ' + cls;
            a.href = ctx + href;
            a.appendChild(icon(iconName));
            var div = document.createElement('div');
            var strong = document.createElement('strong');
            strong.textContent = title;
            var small = document.createElement('small');
            small.textContent = detail;
            div.appendChild(strong);
            div.appendChild(small);
            a.appendChild(div);
            return a;
        }
        getJson('/api/notifications').then(function (data) {
            body.textContent = '';
            var total = (data.lowStock || 0) + (data.nearExpiry || 0) + (data.expired || 0);
            if (data.expired > 0) {
                body.appendChild(row('bad', 'calendar-x', '/inventory?status=expired', data.expired + ' expired batch' + (data.expired === 1 ? '' : 'es'),
                    'Still holding stock - cannot be dispensed'));
            }
            if (data.lowStock > 0) {
                body.appendChild(row('warn', 'exclamation-triangle', '/medicines?stock=low', data.lowStock + ' medicine' + (data.lowStock === 1 ? '' : 's') + ' low on stock',
                    'At or below the reorder level'));
            }
            if (data.nearExpiry > 0) {
                body.appendChild(row('warn', 'clock-history', '/inventory?status=near', data.nearExpiry + ' batch' + (data.nearExpiry === 1 ? '' : 'es') + ' near expiry',
                    'Expiring within ' + data.nearExpiryDays + ' days'));
            }
            if (total === 0) {
                var empty = document.createElement('div');
                empty.className = 'alerts-empty';
                empty.textContent = 'No stock alerts. Everything looks fine.';
                body.appendChild(empty);
            } else {
                $('#alertsDot').classList.remove('d-none');
                $('#alertsButton').setAttribute('aria-label', 'Stock alerts (' + total + ')');
            }
        }).catch(function (error) {
            body.textContent = '';
            var failed = document.createElement('div');
            failed.className = 'alerts-empty';
            failed.textContent = 'Alerts could not be loaded. ' + error.message;
            body.appendChild(failed);
        });
    }

    // ------------------------------------------------------------ autocomplete lookup
    // <div class="lookup" data-lookup="patients"><input type="text" data-lookup-text><input type="hidden" data-lookup-id></div>
    function initLookups() {
        $all('[data-lookup]').forEach(function (box) {
            var input = $('[data-lookup-text]', box);
            var hidden = $('[data-lookup-id]', box);
            var list = document.createElement('ul');
            list.className = 'lookup-list';
            list.hidden = true;
            list.setAttribute('role', 'listbox');
            list.id = 'lookup_' + box.getAttribute('data-lookup') + '_list';
            box.appendChild(list);
            input.setAttribute('role', 'combobox');
            input.setAttribute('aria-autocomplete', 'list');
            input.setAttribute('aria-controls', list.id);
            input.setAttribute('aria-expanded', 'false');
            input.setAttribute('autocomplete', 'off');
            var timer = null;
            var active = -1;
            var chosenLabel = input.value;
            var sequence = 0;

            function close() { list.hidden = true; active = -1; input.setAttribute('aria-expanded', 'false'); }
            function note(text) {
                list.textContent = '';
                var li = document.createElement('li');
                li.className = 'lookup-note';
                li.textContent = text;
                list.appendChild(li);
                list.hidden = false;
                input.setAttribute('aria-expanded', 'true');
            }
            function choose(item) {
                hidden.value = item.id;
                input.value = item.label;
                chosenLabel = item.label;
                input.setCustomValidity('');
                close();
            }
            function render(items) {
                list.textContent = '';
                if (!items.length) { note('No matches found.'); return; }
                items.forEach(function (item) {
                    var li = document.createElement('li');
                    li.setAttribute('role', 'option');
                    var name = document.createElement('span');
                    name.textContent = item.label;
                    var small = document.createElement('small');
                    small.textContent = item.detail;
                    li.appendChild(name);
                    li.appendChild(small);
                    li.addEventListener('mousedown', function (e) { e.preventDefault(); choose(item); });
                    li._item = item;
                    list.appendChild(li);
                });
                list.hidden = false;
                input.setAttribute('aria-expanded', 'true');
            }
            function search() {
                var term = input.value.trim();
                var mine = ++sequence;
                note('Searching...');
                getJson('/api/lookup/' + box.getAttribute('data-lookup') + '?q=' + encodeURIComponent(term))
                    .then(function (items) { if (mine === sequence) { render(items); } })
                    .catch(function (error) { if (mine === sequence) { note(error.message); } });
            }
            input.addEventListener('input', function () {
                if (input.value !== chosenLabel) { hidden.value = ''; }
                window.clearTimeout(timer);
                timer = window.setTimeout(search, 220);
            });
            input.addEventListener('focus', function () { if (!hidden.value) { search(); } });
            input.addEventListener('blur', function () {
                window.setTimeout(close, 120);
                if (input.hasAttribute('required')) {
                    input.setCustomValidity(hidden.value ? '' : 'Select an entry from the list.');
                }
            });
            input.addEventListener('keydown', function (e) {
                var options = $all('li[role="option"]', list);
                if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                    if (list.hidden) { search(); return; }
                    e.preventDefault();
                    if (!options.length) { return; }
                    active = (active + (e.key === 'ArrowDown' ? 1 : -1) + options.length) % options.length;
                    options.forEach(function (o, i) { o.classList.toggle('active', i === active); });
                    options[active].scrollIntoView({ block: 'nearest' });
                } else if (e.key === 'Enter' && !list.hidden && active >= 0 && options[active]) {
                    e.preventDefault();
                    choose(options[active]._item);
                } else if (e.key === 'Escape') {
                    close();
                }
            });
            if (input.hasAttribute('required') && !hidden.value) {
                input.setCustomValidity('Select an entry from the list.');
            }
        });
    }

    // ------------------------------------------------------------ prescription item rows
    function initItemRows() {
        var container = $('#itemRows');
        var template = $('#itemRowTemplate');
        if (!container || !template) { return; }
        function renumber() {
            $all('.item-row', container).forEach(function (row, i) {
                var n = $('.item-number', row);
                if (n) { n.textContent = String(i + 1); }
            });
            var empty = $('#itemRowsEmpty');
            if (empty) { empty.hidden = $all('.item-row', container).length > 0; }
        }
        function add() {
            var fragment = template.content.cloneNode(true);
            container.appendChild(fragment);
            renumber();
            var rows = $all('.item-row', container);
            var select = $('select', rows[rows.length - 1]);
            if (select) { select.focus(); }
        }
        var addButton = $('#addItemRow');
        if (addButton) { addButton.addEventListener('click', add); }
        container.addEventListener('click', function (e) {
            var remove = e.target.closest('[data-remove-row]');
            if (remove) {
                remove.closest('.item-row').remove();
                renumber();
            }
        });
        if (!$all('.item-row', container).length && container.hasAttribute('data-start-with-row')) { add(); }
        renumber();
    }

    // ------------------------------------------------------------ invoice total preview
    // The figures shown while typing are only an estimate; the server recalculates everything on save.
    function initInvoicePreview() {
        var form = $('#invoiceForm');
        if (!form) { return; }
        var rate = parseFloat(form.getAttribute('data-tax-rate')) || 0;
        function cents(value) { return Math.round((parseFloat(value) || 0) * 100); }
        function money(c) {
            return (c / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }
        function recalc() {
            var subtotal = 0, discount = 0;
            $all('[data-line]', form).forEach(function (row) {
                var gross = cents(row.getAttribute('data-gross'));
                var input = $('input[data-discount]', row);
                var d = input ? Math.max(0, cents(input.value)) : 0;
                if (input) {
                    input.setCustomValidity(d > gross ? 'The discount cannot be more than the line amount.' : '');
                }
                subtotal += gross;
                discount += Math.min(d, gross);
                var out = $('[data-line-total]', row);
                if (out) { out.textContent = money(gross - Math.min(d, gross)); }
            });
            var taxable = subtotal - discount;
            var tax = Math.round(taxable * rate / 100);
            var grand = taxable + tax;
            $('#pvSubtotal').textContent = money(subtotal);
            $('#pvDiscount').textContent = money(discount);
            $('#pvTax').textContent = money(tax);
            $('#pvGrand').textContent = money(grand);
            var pay = $('#f_paymentAmount');
            if (pay) { pay.max = (grand / 100).toFixed(2); }
            var full = $('#payFull');
            if (full) { full.setAttribute('data-amount', (grand / 100).toFixed(2)); }
        }
        form.addEventListener('input', recalc);
        var full = $('#payFull');
        if (full) {
            full.addEventListener('click', function () {
                $('#f_paymentAmount').value = full.getAttribute('data-amount');
            });
        }
        recalc();
    }

    // ------------------------------------------------------------ dispensing helpers
    function initDispenseForm() {
        var fill = $('#fillAllQuantities');
        if (fill) {
            fill.addEventListener('click', function () {
                $all('input[data-suggested]').forEach(function (input) { input.value = input.getAttribute('data-suggested'); });
            });
        }
        var clear = $('#clearAllQuantities');
        if (clear) {
            clear.addEventListener('click', function () {
                $all('input[data-suggested]').forEach(function (input) { input.value = '0'; });
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        initSidebar();
        initToasts();
        initForms();
        initModalFill();
        initAlerts();
        initLookups();
        initItemRows();
        initInvoicePreview();
        initDispenseForm();
    });
})();
