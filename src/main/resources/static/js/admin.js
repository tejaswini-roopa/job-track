'use strict';
onReady(() => {
    const NEXT = {
        APPLIED: ['UNDER_REVIEW', 'REJECTED'],
        UNDER_REVIEW: ['ONLINE_ASSESSMENT', 'INTERVIEW', 'REJECTED'],
        ONLINE_ASSESSMENT: ['INTERVIEW', 'REJECTED'],
        INTERVIEW: ['SELECTED', 'REJECTED'],
        SELECTED: [], REJECTED: []
    };
    const jobs = new Map(), companies = new Map(), apps = new Map();
    let companyOptions = null, charts = {}, activeApp = null;
    const modal = id => bootstrap.Modal.getOrCreateInstance($(id));
    const loadingRow = cols => `<tr><td colspan="${cols}" class="text-center py-4"><div class="spinner-border spinner-border-sm text-primary"></div></td></tr>`;
    const emptyRow = (cols, msg) => `<tr><td colspan="${cols}" class="text-center text-muted py-4">${esc(msg)}</td></tr>`;
    const fail = err => showAlert('alertBox', err.message);

    /* ---------- tabs load lazily ---------- */
    const loaded = { overview: true };
    const loaders = {
        overview: () => loadOverview(), jobs: () => loadJobs(0), companies: () => loadCompanies(0),
        applications: () => loadApps(0), students: () => loadStudents(0)
    };
    document.querySelectorAll('#adminTabs [data-key]').forEach(b => b.addEventListener('shown.bs.tab', () => {
        if (!loaded[b.dataset.key]) { loaded[b.dataset.key] = true; loaders[b.dataset.key](); }
    }));

    /* ---------- overview ---------- */
    function draw(key, canvasId, config) {
        if (charts[key]) charts[key].destroy();
        charts[key] = new Chart($(canvasId), config);
    }
    async function loadOverview() {
        try {
            const a = await api('/api/admin/analytics');
            $('aTotal').textContent = a.totalApplications;
            $('aStudents').textContent = a.totalStudents;
            $('aJobs').textContent = a.openJobs;
            $('aRate').textContent = a.successRate + '%';
            $('ratioText').textContent = `Of decided applications: ${a.selectionRate}% selected, ${a.rejectionRate}% rejected`;
            draw('ratio', 'ratioChart', {
                type: 'doughnut',
                data: { labels: ['Selected', 'Rejected'], datasets: [{ data: [a.selected, a.rejected], backgroundColor: ['#198754', '#dc3545'] }] },
                options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
            });
            draw('month', 'monthChart', {
                type: 'line',
                data: { labels: a.monthlyTrend.map(m => m.month), datasets: [{ label: 'Applications', data: a.monthlyTrend.map(m => m.total), borderColor: '#4f46e5', backgroundColor: 'rgba(79,70,229,.15)', fill: true, tension: .3 }] },
                options: { maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
            });
            const top = a.companyStats.slice(0, 8);
            draw('company', 'companyChart', {
                type: 'bar',
                data: { labels: top.map(c => c.company), datasets: [{ label: 'Applications', data: top.map(c => c.total), backgroundColor: '#6366f1' }] },
                options: { indexAxis: 'y', maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, ticks: { precision: 0 } } } }
            });
            const keys = Object.keys(a.statusBreakdown);
            draw('status', 'statusChart', {
                type: 'doughnut',
                data: { labels: keys.map(statusLabel), datasets: [{ data: keys.map(k => a.statusBreakdown[k]), backgroundColor: keys.map(k => STATUS_COLOR[k]) }] },
                options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
            });
        } catch (err) { fail(err); }
    }
    $('refreshOverview').addEventListener('click', loadOverview);
    loadOverview();

    /* ---------- companies ---------- */
    async function ensureCompanyOptions() {
        if (companyOptions) return companyOptions;
        const r = await api('/api/admin/companies?size=50&sort=name,asc');
        companyOptions = r.content;
        return companyOptions;
    }
    async function loadCompanies(page = 0) {
        const body = $('companiesBody');
        body.innerHTML = loadingRow(5);
        try {
            const r = await api('/api/admin/companies' + qs({ q: $('companyFilter').elements.q.value.trim(), page, size: 8 }));
            companies.clear();
            r.content.forEach(c => companies.set(c.id, c));
            body.innerHTML = r.content.length ? r.content.map(c => `<tr>
                <td class="fw-semibold">${esc(c.name)}</td><td>${esc(c.industry || '-')}</td><td>${esc(c.location || '-')}</td>
                <td>${c.website ? safeLink(c.website) : '-'}</td>
                <td class="text-end table-actions">
                    <button class="btn btn-sm btn-outline-primary" data-action="edit" data-id="${c.id}" type="button">Edit</button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${c.id}" type="button">Delete</button>
                </td></tr>`).join('') : emptyRow(5, 'No companies found.');
            renderPagination($('companiesPager'), r, loadCompanies);
        } catch (err) { body.innerHTML = ''; fail(err); }
    }
    $('companyFilter').addEventListener('submit', e => { e.preventDefault(); loadCompanies(0); });
    $('addCompanyBtn').addEventListener('click', () => openCompanyForm(null));

    function openCompanyForm(c) {
        const f = $('companyForm');
        f.reset(); clearFormErrors(f); $('companyAlert').innerHTML = '';
        f.dataset.id = c ? c.id : '';
        $('companyModalTitle').textContent = c ? 'Edit company' : 'Add company';
        if (c) {
            f.elements.name.value = c.name; f.elements.industry.value = c.industry || '';
            f.elements.location.value = c.location || ''; f.elements.website.value = c.website || '';
        }
        modal('companyModal').show();
    }
    $('companiesBody').addEventListener('click', async e => {
        const b = e.target.closest('[data-action]');
        if (!b) return;
        const c = companies.get(Number(b.dataset.id));
        if (b.dataset.action === 'edit') return openCompanyForm(c);
        if (!confirm(`Delete company "${c.name}"?`)) return;
        try {
            await api(`/api/admin/companies/${c.id}`, { method: 'DELETE' });
            companyOptions = null;
            showToast('Company deleted');
            loadCompanies(0);
        } catch (err) { showToast(err.message, 'danger'); }
    });
    $('companyForm').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target, el = f.elements, btn = $('companySubmit');
        clearFormErrors(f); $('companyAlert').innerHTML = '';
        if (!f.checkValidity()) { f.classList.add('was-validated'); return; }
        const id = f.dataset.id;
        setBusy(btn, true);
        try {
            await api(id ? `/api/admin/companies/${id}` : '/api/admin/companies', {
                method: id ? 'PUT' : 'POST',
                body: { name: el.name.value.trim(), industry: nn(el.industry.value), location: nn(el.location.value), website: nn(el.website.value) }
            });
            companyOptions = null;
            modal('companyModal').hide();
            showToast(id ? 'Company updated' : 'Company added');
            loadCompanies(0);
        } catch (err) {
            if (err.status === 400) applyFieldErrors(f, err.fieldErrors);
            showAlert('companyAlert', err.message);
        } finally { setBusy(btn, false); }
    });

    /* ---------- jobs ---------- */
    async function loadJobs(page = 0) {
        const body = $('jobsBody'), f = $('jobFilter').elements;
        body.innerHTML = loadingRow(6);
        try {
            const r = await api('/api/admin/jobs' + qs({
                role: f.role.value.trim(), company: f.company.value.trim(), location: f.location.value.trim(), page, size: 8 }));
            jobs.clear();
            r.content.forEach(j => jobs.set(j.id, j));
            body.innerHTML = r.content.length ? r.content.map(j => `<tr>
                <td class="fw-semibold">${esc(j.title)}<div class="small text-muted">${esc(j.location || '')}</div></td>
                <td>${esc(j.companyName)}</td><td>${esc(typeLabel(j.jobType))}</td><td>${esc(fmtDate(j.applicationDeadline))}</td>
                <td><span class="badge text-bg-${j.status === 'OPEN' ? 'success' : 'secondary'}">${esc(typeLabel(j.status))}</span></td>
                <td class="text-end table-actions">
                    <button class="btn btn-sm btn-outline-primary" data-action="edit" data-id="${j.id}" type="button">Edit</button>
                    <button class="btn btn-sm btn-outline-warning" data-action="toggle" data-id="${j.id}" type="button">${j.status === 'OPEN' ? 'Close' : 'Reopen'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${j.id}" type="button">Delete</button>
                </td></tr>`).join('') : emptyRow(6, 'No jobs found.');
            renderPagination($('jobsPager'), r, loadJobs);
        } catch (err) { body.innerHTML = ''; fail(err); }
    }
    $('jobFilter').addEventListener('submit', e => { e.preventDefault(); loadJobs(0); });
    $('addJobBtn').addEventListener('click', () => openJobForm(null));

    async function openJobForm(j) {
        const f = $('jobForm'), el = f.elements;
        try {
            const opts = await ensureCompanyOptions();
            if (!opts.length) { showToast('Add a company first', 'warning'); return; }
            el.companyId.innerHTML = opts.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
        } catch (err) { return showToast(err.message, 'danger'); }
        f.reset(); clearFormErrors(f); $('jobAlert').innerHTML = '';
        f.dataset.id = j ? j.id : '';
        $('jobModalTitle').textContent = j ? 'Edit job' : 'Add job';
        if (j) {
            el.companyId.value = j.companyId; el.title.value = j.title; el.description.value = j.description;
            el.location.value = j.location || ''; el.jobType.value = j.jobType; el.status.value = j.status;
            el.skillsRequired.value = j.skillsRequired || ''; el.minCgpa.value = j.minCgpa == null ? '' : j.minCgpa;
            el.salaryLpa.value = j.salaryLpa == null ? '' : j.salaryLpa;
            el.applicationDeadline.value = j.applicationDeadline || ''; el.driveDate.value = j.driveDate || '';
        }
        modal('jobModal').show();
    }
    $('jobsBody').addEventListener('click', async e => {
        const b = e.target.closest('[data-action]');
        if (!b) return;
        const j = jobs.get(Number(b.dataset.id));
        try {
            if (b.dataset.action === 'edit') return openJobForm(j);
            if (b.dataset.action === 'toggle') {
                await api(`/api/admin/jobs/${j.id}/status`, { method: 'PATCH', body: { status: j.status === 'OPEN' ? 'CLOSED' : 'OPEN' } });
                showToast('Job status updated');
            } else {
                if (!confirm(`Delete job "${j.title}"?`)) return;
                await api(`/api/admin/jobs/${j.id}`, { method: 'DELETE' });
                showToast('Job deleted');
            }
            loadJobs(0);
        } catch (err) { showToast(err.message, 'danger'); }
    });
    $('jobForm').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target, el = f.elements, btn = $('jobSubmit');
        clearFormErrors(f); $('jobAlert').innerHTML = '';
        if (!f.checkValidity()) { f.classList.add('was-validated'); return; }
        const id = f.dataset.id;
        setBusy(btn, true);
        try {
            await api(id ? `/api/admin/jobs/${id}` : '/api/admin/jobs', {
                method: id ? 'PUT' : 'POST',
                body: {
                    companyId: Number(el.companyId.value), title: el.title.value.trim(), description: el.description.value.trim(),
                    location: nn(el.location.value), jobType: el.jobType.value, skillsRequired: nn(el.skillsRequired.value),
                    minCgpa: num(el.minCgpa.value), salaryLpa: num(el.salaryLpa.value),
                    applicationDeadline: nn(el.applicationDeadline.value), driveDate: nn(el.driveDate.value), status: el.status.value
                }
            });
            modal('jobModal').hide();
            showToast(id ? 'Job updated' : 'Job created');
            loadJobs(0);
        } catch (err) {
            if (err.status === 400) applyFieldErrors(f, err.fieldErrors);
            showAlert('jobAlert', err.message);
        } finally { setBusy(btn, false); }
    });

    /* ---------- applications ---------- */
    async function loadApps(page = 0) {
        const body = $('appsBody');
        body.innerHTML = loadingRow(5);
        try {
            const r = await api('/api/admin/applications' + qs({ status: $('appStatusFilter').value, page, size: 8 }));
            apps.clear();
            r.content.forEach(a => apps.set(a.id, a));
            body.innerHTML = r.content.length ? r.content.map(a => `<tr>
                <td class="fw-semibold">${esc(a.studentName)}<div class="small text-muted">${esc(a.studentEmail)}</div></td>
                <td>${esc(a.jobTitle)}<div class="small text-muted">${esc(a.companyName)}</div></td>
                <td>${statusBadge(a.status)}</td><td>${esc(fmtDate(a.appliedAt))}</td>
                <td class="text-end table-actions">
                    <button class="btn btn-sm btn-outline-primary" data-action="status" data-id="${a.id}" type="button" ${NEXT[a.status].length ? '' : 'disabled'}>Status</button>
                    <button class="btn btn-sm btn-outline-info" data-action="interview" data-id="${a.id}" type="button" ${NEXT[a.status].length ? '' : 'disabled'}>Interview</button>
                    <button class="btn btn-sm btn-outline-secondary" data-action="history" data-id="${a.id}" type="button">History</button>
                </td></tr>`).join('') : emptyRow(5, 'No applications found.');
            renderPagination($('appsPager'), r, loadApps);
        } catch (err) { body.innerHTML = ''; fail(err); }
    }
    $('appStatusFilter').addEventListener('change', () => loadApps(0));

    $('appsBody').addEventListener('click', async e => {
        const b = e.target.closest('[data-action]');
        if (!b) return;
        const a = apps.get(Number(b.dataset.id));
        activeApp = a;
        if (b.dataset.action === 'status') {
            const f = $('statusForm');
            f.reset(); $('statusAlert').innerHTML = '';
            $('statusFor').innerHTML = `${esc(a.studentName)} &middot; ${esc(a.jobTitle)} &middot; currently ${statusBadge(a.status)}`;
            f.elements.status.innerHTML = NEXT[a.status].map(s => `<option value="${s}">${esc(statusLabel(s))}</option>`).join('');
            modal('statusModal').show();
        } else if (b.dataset.action === 'interview') {
            const f = $('interviewForm');
            f.reset(); clearFormErrors(f); $('interviewAlert').innerHTML = '';
            $('interviewFor').textContent = `${a.studentName} \u00b7 ${a.jobTitle} at ${a.companyName}`;
            modal('interviewModal').show();
        } else {
            $('historyBody').innerHTML = '<div class="text-center py-3"><div class="spinner-border text-primary"></div></div>';
            modal('historyModal').show();
            try {
                const h = await api(`/api/admin/applications/${a.id}/history`);
                $('historyBody').innerHTML = h.length ? '<ul class="timeline">' + h.map(x => `
                    <li><div>${x.oldStatus ? statusBadge(x.oldStatus) + ' &rarr; ' : ''}${statusBadge(x.newStatus)}</div>
                    <div class="small text-muted">${esc(fmtDateTime(x.changedAt))}${x.changedBy ? ' &middot; by ' + esc(x.changedBy) : ''}</div>
                    ${x.remarks ? `<div class="small">${esc(x.remarks)}</div>` : ''}</li>`).join('') + '</ul>'
                    : '<p class="text-muted mb-0">No history yet.</p>';
            } catch (err) { $('historyBody').innerHTML = `<div class="text-danger">${esc(err.message)}</div>`; }
        }
    });
    $('statusForm').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target, btn = $('statusSubmit');
        setBusy(btn, true);
        try {
            await api(`/api/admin/applications/${activeApp.id}/status`, {
                method: 'PATCH', body: { status: f.elements.status.value, remarks: nn(f.elements.remarks.value) } });
            modal('statusModal').hide();
            showToast('Status updated');
            loadApps(0);
        } catch (err) { showAlert('statusAlert', err.message); } finally { setBusy(btn, false); }
    });
    $('interviewForm').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target, el = f.elements, btn = $('interviewSubmit');
        clearFormErrors(f); $('interviewAlert').innerHTML = '';
        if (!f.checkValidity()) { f.classList.add('was-validated'); return; }
        setBusy(btn, true);
        try {
            await api(`/api/admin/applications/${activeApp.id}/interviews`, {
                method: 'POST',
                body: { roundName: el.roundName.value.trim(), scheduledAt: el.scheduledAt.value + ':00', mode: el.mode.value, locationOrLink: nn(el.locationOrLink.value) }
            });
            modal('interviewModal').hide();
            showToast('Interview scheduled');
        } catch (err) {
            if (err.status === 400) applyFieldErrors(f, err.fieldErrors);
            showAlert('interviewAlert', err.message);
        } finally { setBusy(btn, false); }
    });

    /* ---------- students ---------- */
    async function loadStudents(page = 0) {
        const body = $('studentsBody');
        body.innerHTML = loadingRow(5);
        try {
            const r = await api('/api/admin/students' + qs({ page, size: 8 }));
            body.innerHTML = r.content.length ? r.content.map(u => `<tr>
                <td class="fw-semibold">${esc(u.fullName)}</td><td>${esc(u.email)}</td><td>${esc(fmtDate(u.createdAt))}</td>
                <td><span class="badge text-bg-${u.enabled ? 'success' : 'secondary'}">${u.enabled ? 'Active' : 'Disabled'}</span></td>
                <td class="text-end table-actions">
                    <button class="btn btn-sm btn-outline-warning" data-action="toggle" data-id="${u.id}" data-enabled="${u.enabled}" type="button">${u.enabled ? 'Disable' : 'Enable'}</button>
                    <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${u.id}" data-name="${esc(u.fullName)}" type="button">Delete</button>
                </td></tr>`).join('') : emptyRow(5, 'No students registered yet.');
            renderPagination($('studentsPager'), r, loadStudents);
        } catch (err) { body.innerHTML = ''; fail(err); }
    }
    $('studentsBody').addEventListener('click', async e => {
        const b = e.target.closest('[data-action]');
        if (!b) return;
        const id = b.dataset.id;
        try {
            if (b.dataset.action === 'toggle') {
                await api(`/api/admin/students/${id}/enabled`, { method: 'PATCH', body: { enabled: b.dataset.enabled !== 'true' } });
                showToast('Account updated');
            } else {
                if (!confirm(`Delete ${b.dataset.name} and all of their applications? This cannot be undone.`)) return;
                await api(`/api/admin/students/${id}`, { method: 'DELETE' });
                showToast('Student deleted');
            }
            loadStudents(0);
        } catch (err) { showToast(err.message, 'danger'); }
    });
});
