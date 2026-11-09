'use strict';
onReady(() => {
    const u = Auth.user();
    if (u && Auth.token()) {
        $('ctaBox').innerHTML = `<a class="btn btn-primary btn-lg" href="${Auth.homeFor(u.role)}">Go to my dashboard</a>`;
    }
});
