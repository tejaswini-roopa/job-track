'use strict';
onReady(() => {
    const form = $('loginForm'), alertBox = $('alertBox');
    if (new URLSearchParams(location.search).get('expired')) {
        showAlert(alertBox, 'Your session has ended. Please log in again.', 'warning');
    }
    form.addEventListener('submit', async e => {
        e.preventDefault();
        alertBox.innerHTML = '';
        if (!form.checkValidity()) { form.classList.add('was-validated'); return; }
        const btn = $('submitBtn');
        setBusy(btn, true);
        try {
            const r = await api('/api/auth/login', {
                method: 'POST', auth: false,
                body: { email: form.elements.email.value.trim(), password: form.elements.password.value }
            });
            Auth.save(r);
            location.href = Auth.homeFor(r.role);
        } catch (err) {
            let msg = err.message;
            if (err.status === 401) {
                msg = /disabled/i.test(err.message)
                    ? 'This account has been disabled. Please contact the placement cell.'
                    : 'Invalid email or password.';
            }
            showAlert(alertBox, msg);
            setBusy(btn, false);
        }
    });
});
