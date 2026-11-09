'use strict';
onReady(() => {
    const STAGES = ['APPLIED', 'UNDER_REVIEW', 'ONLINE_ASSESSMENT', 'INTERVIEW', 'SELECTED'];
    const apps = new Map();
    let notesId = null, currentPage = 0;
    const list = $('appList'), pager = $('pager');

    function stepper(status) {
        const idx = STAGES.indexOf(status);
        return `<div class="stepper${status === 'REJECTED' ? ' is-rejected' : ''}">` + STAGES.map((s, i) =>
            `<div class="step${i < idx ? ' done' : ''}${i === idx ? ' current' : ''}"><span class="dot"></span><span class="d-none d-md-inline">${esc(statusLabel(s))}</span></div>`
        ).join('') + '</div>';
    }

    function card(a) {
        return `<div class="col-lg-6"><div class="card shadow-sm border-0 h-100"><div class="card-body">
            <div class="d-flex justify-content-between align-items-start gap-2">
                <div><h5 class="mb-0"><a class="text-decoration-none" href="/jobs/${a.jobId}">${esc(a.jobTitle)}</a></h5>
                <div class="text-muted">${esc(a.companyName)}</div></div>
                ${statusBadge(a.status)}
            </div>
            ${stepper(a.status)}
            <div class="small text-muted">Applied ${esc(fmtDate(a.appliedAt))} &middot; Updated ${esc(fmtDate(a.updatedAt))}</div>
            <p class="small mt-2 mb-3 preline">${a.notes ? esc(a.notes.length > 140 ? a.notes.slice(0, 140) + '...' : a.notes) : '<span class="text-muted">No notes yet.</span>'}</p>
            <div class="d-flex gap-2">
                <button class="btn btn-sm btn-outline-primary" data-action="notes" data-id="${a.id}" type="button">Notes</button>
                <button class="btn btn-sm btn-outline-secondary" data-action="history" data-id="${a.id}" type="button">History</button>
            </div>
        </div></div></div>`;
    }

    async function load(page = 0) {
        currentPage = page;
        list.innerHTML = loadingHtml;
        $('alertBox').innerHTML = '';
        try {
            const r = await api('/api/applications' + qs({ status: $('statusFilter').value, page, size: 8, sort: 'appliedAt,desc' }));
            apps.clear();
            r.content.forEach(a => apps.set(a.id, a));
            list.innerHTML = r.content.length ? r.content.map(card).join('')
                : emptyHtml('No applications here yet. Find a job you like on the Jobs page.');
            renderPagination(pager, r, load);
        } catch (err) {
            list.innerHTML = '';
            showAlert('alertBox', err.message);
        }
    }

    $('statusFilter').addEventListener('change', () => load(0));

    list.addEventListener('click', async e => {
        const btn = e.target.closest('[data-action]');
        if (!btn) return;
        const a = apps.get(Number(btn.dataset.id));
        if (btn.dataset.action === 'notes') {
            notesId = a.id;
            $('notesAlert').innerHTML = '';
            $('notesFor').textContent = `${a.jobTitle} at ${a.companyName}`;
            $('notesText').value = a.notes || '';
            bootstrap.Modal.getOrCreateInstance($('notesModal')).show();
        } else {
            $('historyBody').innerHTML = '<div class="text-center py-3"><div class="spinner-border text-primary"></div></div>';
            bootstrap.Modal.getOrCreateInstance($('historyModal')).show();
            try {
                const h = await api(`/api/applications/${a.id}/history`);
                $('historyBody').innerHTML = h.length ? '<ul class="timeline">' + h.map(x => `
                    <li><div>${x.oldStatus ? statusBadge(x.oldStatus) + ' &rarr; ' : ''}${statusBadge(x.newStatus)}</div>
                    <div class="small text-muted">${esc(fmtDateTime(x.changedAt))}${x.changedBy ? ' &middot; by ' + esc(x.changedBy) : ''}</div>
                    ${x.remarks ? `<div class="small">${esc(x.remarks)}</div>` : ''}</li>`).join('') + '</ul>'
                    : '<p class="text-muted mb-0">No history yet.</p>';
            } catch (err) {
                $('historyBody').innerHTML = `<div class="text-danger">${esc(err.message)}</div>`;
            }
        }
    });

    $('saveNotes').addEventListener('click', async () => {
        const btn = $('saveNotes');
        setBusy(btn, true);
        try {
            await api(`/api/applications/${notesId}/notes`, { method: 'PATCH', body: { notes: $('notesText').value } });
            bootstrap.Modal.getOrCreateInstance($('notesModal')).hide();
            showToast('Notes saved');
            load(currentPage);
        } catch (err) {
            showAlert('notesAlert', err.message);
        } finally {
            setBusy(btn, false);
        }
    });

    load(0);
});
