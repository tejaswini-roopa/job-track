'use strict';
onReady(() => {
    const PAGE_SIZE = 9;
    const state = { tab: 'all' };
    let savedIds = new Set(), appliedIds = new Set();
    const form = $('filterForm'), list = $('jobList'), pager = $('pager'), summary = $('summary');

    async function loadMarkers() {
        try {
            const [s, a] = await Promise.all([
                api('/api/student/saved-jobs?size=50'), api('/api/applications?size=50')]);
            savedIds = new Set(s.content.map(x => x.job.id));
            appliedIds = new Set(a.content.map(x => x.jobId));
        } catch (e) { /* the saved/applied markers are cosmetic */ }
    }

    function url(page) {
        if (state.tab === 'saved') return '/api/student/saved-jobs' + qs({ page, size: PAGE_SIZE });
        const f = form.elements;
        return '/api/jobs' + qs({
            company: f.company.value.trim(), role: f.role.value.trim(), location: f.location.value.trim(),
            skills: f.skills.value.trim(), jobType: f.jobType.value,
            eligibleOnly: f.eligibleOnly.checked ? 'true' : '', sort: f.sort.value, page, size: PAGE_SIZE
        });
    }

    function saveButton(id) {
        const saved = savedIds.has(id);
        return `<button type="button" class="btn btn-sm ${saved ? 'btn-warning' : 'btn-outline-warning'} save-btn" data-id="${id}">${saved ? '&#9733; Saved' : '&#9734; Save'}</button>`;
    }

    function jobCard(j) {
        const skills = (j.skillsRequired || '').split(',').map(s => s.trim()).filter(Boolean).slice(0, 4)
            .map(s => `<span class="badge text-bg-light border me-1">${esc(s)}</span>`).join('');
        return `<div class="col-md-6 col-xl-4"><div class="card h-100 job-card shadow-sm"><div class="card-body d-flex flex-column">
            <div class="d-flex justify-content-between">
                <span class="badge text-bg-light border">${esc(typeLabel(j.jobType))}</span>
                ${appliedIds.has(j.id) ? '<span class="badge text-bg-success">Applied</span>' : ''}
            </div>
            <h5 class="mt-2 mb-0"><a href="/jobs/${j.id}">${esc(j.title)}</a></h5>
            <div class="text-muted mb-2">${esc(j.companyName)} &middot; ${esc(j.location || 'Location not specified')}</div>
            <div class="mb-2">${skills}</div>
            <ul class="list-unstyled small text-muted mb-3">
                <li>Salary: ${j.salaryLpa != null ? esc(j.salaryLpa) + ' LPA' : 'Not disclosed'}</li>
                <li>Minimum CGPA: ${esc(j.minCgpa != null ? j.minCgpa : 0)}</li>
                <li>Apply by: ${esc(fmtDate(j.applicationDeadline))}</li>
            </ul>
            <div class="mt-auto d-flex gap-2">
                <a class="btn btn-sm btn-primary" href="/jobs/${j.id}">View details</a>
                <span data-save-slot="${j.id}">${saveButton(j.id)}</span>
            </div>
        </div></div></div>`;
    }

    async function load(page = 0) {
        list.innerHTML = loadingHtml;
        $('alertBox').innerHTML = '';
        try {
            const r = await api(url(page));
            const jobs = state.tab === 'saved' ? r.content.map(x => x.job) : r.content;
            summary.textContent = `${r.totalElements} job(s) ${state.tab === 'saved' ? 'in your wishlist' : 'found'}`;
            list.innerHTML = jobs.length ? jobs.map(jobCard).join('')
                : emptyHtml(state.tab === 'saved' ? 'Your wishlist is empty.' : 'No jobs match your search.');
            renderPagination(pager, r, load);
        } catch (err) {
            list.innerHTML = '';
            showAlert('alertBox', err.message);
        }
    }

    form.addEventListener('submit', e => { e.preventDefault(); load(0); });
    ['jobType', 'sort', 'eligibleOnly'].forEach(n => form.elements[n].addEventListener('change', () => load(0)));
    $('resetBtn').addEventListener('click', () => { form.reset(); load(0); });

    $('tabs').addEventListener('click', e => {
        const b = e.target.closest('[data-tab]');
        if (!b || b.dataset.tab === state.tab) return;
        state.tab = b.dataset.tab;
        $('tabs').querySelectorAll('.nav-link').forEach(x => x.classList.toggle('active', x === b));
        form.classList.toggle('d-none', state.tab === 'saved');
        load(0);
    });

    list.addEventListener('click', async e => {
        const btn = e.target.closest('.save-btn');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        btn.disabled = true;
        try {
            if (savedIds.has(id)) {
                await api(`/api/student/saved-jobs/${id}`, { method: 'DELETE' });
                savedIds.delete(id);
                showToast('Removed from wishlist', 'secondary');
            } else {
                await api(`/api/student/saved-jobs/${id}`, { method: 'POST' });
                savedIds.add(id);
                showToast('Saved to wishlist');
            }
            if (state.tab === 'saved') load(0);
            else btn.closest('[data-save-slot]').innerHTML = saveButton(id);
        } catch (err) {
            showToast(err.message, 'danger');
            btn.disabled = false;
        }
    });

    loadMarkers().then(() => load(0));
});
