'use strict';
onReady(async () => {
    const form = $('profileForm'), els = form.elements, alertBox = $('alertBox');
    const FIELDS = ['fullName', 'phone', 'college', 'degree', 'branch', 'graduationYear', 'cgpa', 'skills', 'resumeUrl'];

    if (new URLSearchParams(location.search).get('welcome')) {
        showAlert(alertBox, 'Account created! Add your CGPA and skills so we can show you eligible jobs.', 'success');
    }
    try {
        const p = await api('/api/student/profile');
        els.email.value = p.email;
        FIELDS.forEach(f => { els[f].value = p[f] == null ? '' : p[f]; });
        form.classList.remove('d-none');
    } catch (err) {
        showAlert(alertBox, err.message);
    } finally {
        $('loading').classList.add('d-none');
    }

    form.addEventListener('submit', async e => {
        e.preventDefault();
        clearFormErrors(form);
        if (!form.checkValidity()) { form.classList.add('was-validated'); return; }
        const btn = $('submitBtn');
        setBusy(btn, true);
        try {
            const p = await api('/api/student/profile', {
                method: 'PUT',
                body: {
                    fullName: els.fullName.value.trim(), phone: nn(els.phone.value), college: nn(els.college.value),
                    degree: nn(els.degree.value), branch: nn(els.branch.value), graduationYear: num(els.graduationYear.value),
                    cgpa: num(els.cgpa.value), skills: nn(els.skills.value), resumeUrl: nn(els.resumeUrl.value)
                }
            });
            Auth.updateName(p.fullName);
            renderNav();
            showAlert(alertBox, 'Profile saved.', 'success');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (err) {
            if (err.status === 400) applyFieldErrors(form, err.fieldErrors);
            showAlert(alertBox, err.message);
        } finally {
            setBusy(btn, false);
        }
    });
});
