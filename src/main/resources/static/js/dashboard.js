'use strict';
onReady(async () => {
    $('welcome').textContent = 'Welcome back, ' + Auth.user().fullName;
    try {
        const d = await api('/api/student/dashboard');
        $('statTotal').textContent = d.totalApplications;
        $('statReview').textContent = d.underReview;
        $('statOA').textContent = d.onlineAssessment;
        $('statInterview').textContent = d.interview;
        $('statSelected').textContent = d.selected;
        $('statRejected').textContent = d.rejected;

        if (d.totalApplications === 0) {
            $('chartsRow').classList.add('d-none');
            $('noApps').classList.remove('d-none');
        } else {
            const keys = Object.keys(d.statusBreakdown);
            new Chart($('statusChart'), {
                type: 'doughnut',
                data: {
                    labels: keys.map(statusLabel),
                    datasets: [{ data: keys.map(k => d.statusBreakdown[k]), backgroundColor: keys.map(k => STATUS_COLOR[k]) }]
                },
                options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
            });
            new Chart($('trendChart'), {
                type: 'bar',
                data: {
                    labels: d.monthlyTrend.map(m => m.month),
                    datasets: [{ label: 'Applications', data: d.monthlyTrend.map(m => m.total), backgroundColor: '#4f46e5' }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
                }
            });
        }

        $('interviews').innerHTML = d.upcomingInterviews.length
            ? '<ul class="list-group list-group-flush">' + d.upcomingInterviews.map(i => `
                <li class="list-group-item px-0">
                    <div class="d-flex justify-content-between flex-wrap">
                        <strong>${esc(i.roundName)}</strong>
                        <span class="text-muted">${esc(fmtDateTime(i.scheduledAt))}</span>
                    </div>
                    <div class="small">${esc(i.jobTitle)} at ${esc(i.companyName)} &middot; ${esc(typeLabel(i.mode))}</div>
                    <div class="small">${safeLink(i.locationOrLink)}</div>
                </li>`).join('') + '</ul>'
            : '<p class="text-muted mb-0">No interviews scheduled.</p>';
        $('content').classList.remove('d-none');
    } catch (err) {
        showAlert('alertBox', err.message);
    } finally {
        $('loading').classList.add('d-none');
    }
});
