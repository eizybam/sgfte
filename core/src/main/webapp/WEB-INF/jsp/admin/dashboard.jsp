<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Analíticas"/>
<c:set var="pageSubtitle" value="Indicadores del sistema en tiempo real"/>
<c:set var="activeNav" value="analytics"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="kpi-grid" style="margin-bottom: var(--sp-3);">
    <div class="card kpi">
        <span class="card__label">Concentradora</span>
        <div class="money money--lg">$ ${concentratorBalance}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">En cuentas</span>
        <div class="money money--lg">$ ${totalInAccounts}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">Tarjetahabientes</span>
        <div class="money money--lg">${activeCardholders}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">Cuentas activas</span>
        <div class="money money--lg">${activeAccounts}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">Tarjetas activas</span>
        <div class="money money--lg">${activeCards}</div>
    </div>
</div>

<div class="card">
    <span class="card__label">Movimientos por tipo</span>
    <div style="margin-top: var(--sp-3);">
        <canvas id="chart" height="120"></canvas>
    </div>
    <p id="chart-empty" class="empty" style="display:none;">Todavía no hay movimientos que graficar.</p>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
<script>
    // El microservicio de analítica entrega JSON; la gráfica se dibuja en el cliente (RF-11).
    fetch('${pageContext.request.contextPath}/admin/analytics.json')
        .then(function (r) { return r.json(); })
        .then(function (data) {
            var byType = data.movementsByType || {};
            var labels = Object.keys(byType);

            if (labels.length === 0) {
                document.getElementById('chart').style.display = 'none';
                document.getElementById('chart-empty').style.display = 'block';
                return;
            }

            new Chart(document.getElementById('chart'), {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Movimientos',
                        data: labels.map(function (k) { return byType[k]; }),
                        backgroundColor: '#c34100',
                        borderRadius: 4
                    }]
                },
                options: {
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { ticks: { color: '#e1bfb4' }, grid: { color: '#2b2a28' } },
                        y: { ticks: { color: '#e1bfb4' }, grid: { color: '#2b2a28' }, beginAtZero: true }
                    }
                }
            });
        })
        .catch(function () {
            // Tolerancia a fallos: si Analítica no responde, la página no se rompe.
            document.getElementById('chart').style.display = 'none';
            var msg = document.getElementById('chart-empty');
            msg.textContent = 'El servicio de analítica no está disponible en este momento.';
            msg.style.display = 'block';
        });
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
