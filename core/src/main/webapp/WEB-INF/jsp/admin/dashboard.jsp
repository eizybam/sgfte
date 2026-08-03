<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 900px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-4); }
        .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: var(--sp-2); margin-bottom: var(--sp-4); }
        .kpi { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-3); }
        .kpi .label { font-family: var(--sgfte-font-mono); font-size: 12px; letter-spacing: .6px; text-transform: uppercase; color: var(--sgfte-tan); }
        .kpi .value { font-family: var(--sgfte-font-mono); font-size: 26px; color: var(--sgfte-salmon); margin-top: var(--sp-1); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <h1>Panel de administración</h1>

    <div class="kpis">
        <div class="kpi"><div class="label">Concentradora</div><div class="value">$ ${concentratorBalance}</div></div>
        <div class="kpi"><div class="label">En cuentas</div><div class="value">$ ${totalInAccounts}</div></div>
        <div class="kpi"><div class="label">Tarjetahabientes</div><div class="value">${activeCardholders}</div></div>
        <div class="kpi"><div class="label">Cuentas activas</div><div class="value">${activeAccounts}</div></div>
        <div class="kpi"><div class="label">Tarjetas activas</div><div class="value">${activeCards}</div></div>
    </div>

    <div class="card">
        <div class="label" style="font-family: var(--sgfte-font-mono); font-size:12px; text-transform:uppercase; color: var(--sgfte-tan); margin-bottom: var(--sp-2);">Movimientos por tipo</div>
        <canvas id="chart" height="120"></canvas>
    </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
<script>
    fetch('${pageContext.request.contextPath}/admin/analytics.json')
        .then(function (r) { return r.json(); })
        .then(function (data) {
            var byType = data.movementsByType || {};
            var labels = Object.keys(byType);
            var values = labels.map(function (k) { return byType[k]; });
            new Chart(document.getElementById('chart'), {
                type: 'bar',
                data: { labels: labels, datasets: [{ label: 'Movimientos', data: values, backgroundColor: '#c34100' }] },
                options: { plugins: { legend: { display: false } },
                           scales: { x: { ticks: { color: '#e1bfb4' } }, y: { ticks: { color: '#e1bfb4' }, beginAtZero: true } } }
            });
        });
</script>
</body>
</html>
