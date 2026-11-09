'use strict';
onReady(async () => {
    const jobId = Number(document.body.dataset.jobId);
    const applyBtn = $('applyBtn'), saveBtn = $('saveBtn'), alertBox = $('alertBox');
    let saved = false, applied = false, job = null;

    function paintSave() {
        saveBtn.innerHTML = saved ? '&#9733; Saved to wishlist' : '&#9734; Save to wishlist';
        saveBtn.className = 'btn ' + (saved ? 'btn-warning' : 'btn-outline-warning');
    }
    function paintApply() {
        const closed = job.status !== 'OPEN' ||
            (job.applicationDeadline && new Date(job.applicationDeadline + 'T23:59:59') < new Date());
        applyBtn.disabled = applied || closed;
        applyBtn.textContent = applied ? 'Already applied' : (closed ? 'Applications closed' : 'Apply now');
    }

    try {
        const [j, profile, s, a] = await Promise.all([
            api(`/api/jobs/${jobId}`), api('/api/student/profile'),
            api('/api/student/saved-jobs?size=50'), api('/api/applications?size=50')]);
        job = j;
        saved = s.content.some(x => x.job.id === jobId);
        applied = a.content.some(x => x.jobId === jobId);

        $('jobType').textContent = typeLabel(j.jobType);
        $('title').textContent = j.title;
        $('subtitle').textContent = `${j.companyName} \u00b7 ${j.location || 'Location not specified'}`;
        $('description').textContent = j.description;
        $('skills').innerHTML = (j.skillsRequired || '').split(',').map(x => x.trim()).filter(Boolean)
            .map(x => `<span class="badge text-bg-primary me-1">${esc(x)}</span>`).join('') || '<span class="text-muted">Not specified</span>';
        $('salary').textContent = j.salaryLpa != null ? j.salaryLpa + ' LPA' : 'Not disclosed';
        $('minCgpa').textContent = j.minCgpa != null ? j.minCgpa : 0;
        $('deadline').textContent = fmtDate(j.applicationDeadline);
        $('driveDate').textContent = fmtDate(j.driveDate);
        document.title = j.title + ' | JobTrack';

        const required = Number(j.minCgpa || 0), mine = profile.cgpa == null ? null : Number(profile.cgpa);
        if (mine == null && required > 0) {
            $('eligibility').innerHTML = '<div class="alert alert-warning small">Add your CGPA in your <a href="/profile">profile</a> to check eligibility.</div>';
        } else if (mine != null && mine < required) {
            $('eligibility').innerHTML = `<div class="alert alert-danger small">Your CGPA (${esc(mine)}) is below the required ${esc(required)}.</div>`;
        } else {
            $('eligibility').innerHTML = '<div class="alert alert-success small">You meet the CGPA requirement.</div>';
        }
        paintSave();
        paintApply();
        $('content').classList.remove('d-none');
    } catch (err) {
        showAlert(alertBox, err.status === 404 ? 'This job does not exist.' : err.message);
    } finally {
        $('loading').classList.add('d-none');
    }

    applyBtn.addEventListener('click', async () => {
        alertBox.innerHTML = '';
        setBusy(applyBtn, true);
        try {
            await api('/api/applications', { method: 'POST', body: { jobId } });
            applied = true;
            showToast('Application submitted. Good luck!');
        } catch (err) {
            showAlert(alertBox, err.message);
        } finally {
            setBusy(applyBtn, false);
            paintApply();
        }
    });

    saveBtn.addEventListener('click', async () => {
        saveBtn.disabled = true;
        try {
            await api(`/api/student/saved-jobs/${jobId}`, { method: saved ? 'DELETE' : 'POST' });
            saved = !saved;
            paintSave();
            showToast(saved ? 'Saved to wishlist' : 'Removed from wishlist', saved ? 'success' : 'secondary');
        } catch (err) {
            showToast(err.message, 'danger');
        } finally {
            saveBtn.disabled = false;
        }
    });
});
