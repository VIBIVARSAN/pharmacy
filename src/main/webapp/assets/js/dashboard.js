/*
 * Dashboard charts. All numbers are fetched from the backend JSON API
 * (/api/dashboard/...), which reads them from MySQL. When there is no data the
 * chart area shows an empty state instead of an invented picture.
 */
(function () {
    'use strict';

    var FONT = '"Times New Roman", Times, serif';
    var NAVY = '#12294a';
    var TEAL = '#0b7a77';
    var PALETTE = ['#0b7a77', '#12294a', '#3b6fb6', '#7fd1cd', '#8a5a00', '#6b7a90', '#a3261b', '#2f9e6b', '#c08a2e', '#4a5d7a'];

    function setState(box, kind, message) {
        var state = box.querySelector('.chart-state');
        var canvas = box.querySelector('canvas');
        if (kind === 'ready') {
            state.hidden = true;
            canvas.hidden = false;
            return;
        }
        canvas.hidden = true;
        state.hidden = false;
        state.textContent = '';
        if (kind === 'loading') {
            var spinner = document.createElement('span');
            spinner.className = 'spinner-border';
            spinner.setAttribute('aria-hidden', 'true');
            state.appendChild(spinner);
        }
        var text = document.createElement('span');
        text.textContent = message;
        state.appendChild(text);
    }

    function shortDate(iso) {
        var parts = iso.split('-');
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return parseInt(parts[2], 10) + ' ' + months[parseInt(parts[1], 10) - 1];
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (!window.Chart) { return; }
        Chart.defaults.font.family = FONT;
        Chart.defaults.font.size = 14;
        Chart.defaults.color = '#55627a';

        var currency = document.body.getAttribute('data-currency') || '';

        // ---------------------------------------------------- sales trend
        var salesBox = document.getElementById('salesChartBox');
        var salesChart = null;
        function loadSales(days) {
            setState(salesBox, 'loading', 'Loading sales...');
            window.pmsGetJson('/api/dashboard/sales-trend?days=' + days).then(function (data) {
                if (data.empty) {
                    if (salesChart) { salesChart.destroy(); salesChart = null; }
                    setState(salesBox, 'empty', 'No sales were invoiced in the last ' + days + ' days.');
                    return;
                }
                setState(salesBox, 'ready');
                var labels = data.labels.map(shortDate);
                if (salesChart) {
                    salesChart.data.labels = labels;
                    salesChart.data.datasets[0].data = data.values;
                    salesChart.update();
                    return;
                }
                salesChart = new Chart(salesBox.querySelector('canvas'), {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Sales',
                            data: data.values,
                            borderColor: TEAL,
                            backgroundColor: 'rgba(11, 122, 119, 0.12)',
                            pointBackgroundColor: TEAL,
                            pointRadius: 2,
                            pointHoverRadius: 5,
                            borderWidth: 2,
                            tension: 0,
                            fill: true
                        }]
                    },
                    options: {
                        maintainAspectRatio: false,
                        interaction: { mode: 'index', intersect: false },
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    label: function (item) {
                                        return ' ' + currency + ' ' + Number(item.parsed.y).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                                    }
                                }
                            }
                        },
                        scales: {
                            x: { grid: { display: false }, ticks: { maxTicksLimit: 10, maxRotation: 0 } },
                            y: { beginAtZero: true, grid: { color: '#e8ecf1' }, ticks: { maxTicksLimit: 6 } }
                        }
                    }
                });
            }).catch(function (error) {
                setState(salesBox, 'error', 'The sales chart could not be loaded. ' + error.message);
            });
        }
        if (salesBox) {
            var buttons = Array.prototype.slice.call(document.querySelectorAll('[data-sales-range]'));
            buttons.forEach(function (button) {
                button.addEventListener('click', function () {
                    buttons.forEach(function (b) { b.setAttribute('aria-pressed', b === button ? 'true' : 'false'); });
                    loadSales(button.getAttribute('data-sales-range'));
                });
            });
            loadSales(7);
        }

        // ---------------------------------------------------- stock by category
        var stockBox = document.getElementById('stockChartBox');
        if (stockBox) {
            setState(stockBox, 'loading', 'Loading stock...');
            window.pmsGetJson('/api/dashboard/stock-by-category').then(function (data) {
                if (data.empty) {
                    setState(stockBox, 'empty', 'No usable stock on hand yet. Receive stock to see it here.');
                    return;
                }
                setState(stockBox, 'ready');
                new Chart(stockBox.querySelector('canvas'), {
                    type: 'bar',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Units in stock',
                            data: data.values,
                            backgroundColor: data.labels.map(function (_, i) { return PALETTE[i % PALETTE.length]; }),
                            borderRadius: 5,
                            maxBarThickness: 26
                        }]
                    },
                    options: {
                        indexAxis: 'y',
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } },
                        scales: {
                            x: { beginAtZero: true, grid: { color: '#e8ecf1' }, ticks: { precision: 0, maxTicksLimit: 6 } },
                            y: { grid: { display: false }, ticks: { color: NAVY } }
                        }
                    }
                });
            }).catch(function (error) {
                setState(stockBox, 'error', 'The stock chart could not be loaded. ' + error.message);
            });
        }
    });
})();
