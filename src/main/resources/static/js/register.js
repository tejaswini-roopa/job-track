'use strict';
onReady(() => {
    const form = $('registerForm'), alertBox = $('alertBox'), els = form.elements;

    function checkMatch() {
        els.confirm.setCustomValidity(els.confirm.value === els.password.value ? '' : 'mismatch');
    }
    els.password.addEventListener('input', checkMatch);
    els.confirm.addEventListener('input', checkMatch);

    form.addEventListener('submit', async e => {
        e.preventDefault();
        alertBox.innerHTML = '';
        clearFormErrors(form);
        checkMatch();
        if (!form.checkValidity()) { form.classList.add('was-validated'); return; }
        const btn = $('submitBtn');
        setBusy(btn, true);
        const email = els.email.value.trim(), password = els.password.value;
        try {
            await api('/api/auth/register', {
                method: 'POST', auth: false,
                body: { fullName: els.fullName.value.trim(), email, password }
            });
            const r = await api('/api/auth/login', { method: 'POST', auth: false, body: { email, password } });
            Auth.save(r);
            location.href = '/profile?welcome=1';
        } catch (err) {
            if (err.status === 400) applyFieldErrors(form, err.fieldErrors);
            showAlert(alertBox, err.message);
            setBusy(btn, false);
        }
    });
});
