'use strict';

/* ---------- auth storage ---------- */
const TOKEN_KEY = 'jt_token';
const USER_KEY = 'jt_user';

const Auth = {
    token() { return localStorage.getItem(TOKEN_KEY); },
    user() {
        try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch (e) { return null; }
    },
    save(r) {
        localStorage.setItem(TOKEN_KEY, r.token);
        localStorage.setItem(USER_KEY, JSON.stringify(
            { id: r.userId, email: r.email, fullName: r.fullName, role: r.role }));
    },
    updateName(fullName) {
        const u = Auth.user();
        if (u) { u.fullName = fullName; localStorage.setItem(USER_KEY, JSON.stringify(u)); }
    },
    clear() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); },
    homeFor(role) { return role === 'ADMIN' ? '/admin' : '/dashboard'; }
};

/* ---------- fetch wrapper ---------- */
class ApiError extends Error {
    constructor(status, data) {
        super((data && data.message) || 'Something went wrong. Please try again.');
        this.status = status;
        this.fieldErrors = (data && data.fieldErrors) || {};
    }
}

async function api(path, opts = {}) {
    const { method = 'GET', body, auth = true } = opts;
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth && Auth.token()) headers['Authorization'] = 'Bearer ' + Auth.token();

    let res;
    try {
        res = await fetch(path, {
            method, headers, body: body !== undefined ? JSON.stringify(body) : undefined
        });
    } catch (e) {
        throw new ApiError(0, { message: 'Cannot reach the server. Check your connection.' });
    }
    if (res.status === 204) return null;

    let data = null;
    try { data = await res.json(); } catch (e) { /* response had no JSON body */ }

    if (!res.ok) {
        if (res.status === 401 && auth) {            // token missing, expired or account disabled
            Auth.clear();
            window.location.href = '/login?expired=1';
        }
        throw new ApiError(res.status, data);
    }
    return data;
}

/* ---------- small helpers ---------- */
const $ = id => document.getElementById(id);

const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g,
    c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function qs(params) {
    const p = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') p.append(k, v);
    });
    const s = p.toString();
    return s ? '?' + s : '';
}

function fmtDate(v) {
    if (!v) return '-';
    const d = v.length === 10 ? new Date(v + 'T00:00:00') : new Date(v);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function fmtDateTime(v) {
    if (!v) return '-';
    return new Date(v).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

const nn = v => (v == null || String(v).trim() === '') ? null : String(v).trim();
const num = v => (v === '' || v == null) ? null : Number(v);

function safeLink(url) {
    if (url && /^https?:\/\//i.test(url)) {
        return `<a href="${esc(url)}" target="_blank" rel="noopener noreferrer">${esc(url)}</a>`;
    }
    return esc(url || '');
}

const STATUS_STYLE = {
    APPLIED: 'secondary', UNDER_REVIEW: 'info', ONLINE_ASSESSMENT: 'warning',
    INTERVIEW: 'primary', SELECTED: 'success', REJECTED: 'danger'
};
const STATUS_COLOR = {
    APPLIED: '#6c757d', UNDER_REVIEW: '#0dcaf0', ONLINE_ASSESSMENT: '#ffc107',
    INTERVIEW: '#0d6efd', SELECTED: '#198754', REJECTED: '#dc3545'
};
const typeLabel = s => String(s || '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const statusLabel = typeLabel;
const statusBadge = s => `<span class="badge text-bg-${STATUS_STYLE[s] || 'secondary'}">${esc(statusLabel(s))}</span>`;

const loadingHtml = '<div class="col-12 text-center py-5"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';
const emptyHtml = msg => `<div class="col-12 text-center text-muted py-5">${esc(msg)}</div>`;

/* ---------- UI feedback ---------- */
function showAlert(target, message, type = 'danger') {
    const el = typeof target === 'string' ? $(target) : target;
    if (!el) return;
    el.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">${esc(message)}` +
        '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button></div>';
}

function showToast(message, type = 'success') {
    let box = $('toastBox');
    if (!box) {
        box = document.createElement('div');
        box.id = 'toastBox';
        box.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(box);
    }
    const t = document.createElement('div');
    t.className = `toast align-items-center text-bg-${type} border-0`;
    t.setAttribute('role', 'alert');
    t.innerHTML = `<div class="d-flex"><div class="toast-body">${esc(message)}</div>` +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    box.appendChild(t);
    t.addEventListener('hidden.bs.toast', () => t.remove());
    new bootstrap.Toast(t, { delay: 3500 }).show();
}

function setBusy(btn, busy) {
    if (busy) {
        btn.dataset.label = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Please wait...';
    } else {
        btn.disabled = false;
        if (btn.dataset.label) btn.innerHTML = btn.dataset.label;
    }
}

function clearFormErrors(form) {
    form.classList.remove('was-validated');
    form.querySelectorAll('.is-invalid').forEach(e => e.classList.remove('is-invalid'));
}

/** Shows server-side validation messages (ApiError.fieldErrors) next to the matching inputs. */
function applyFieldErrors(form, errors) {
    Object.entries(errors || {}).forEach(([name, message]) => {
        const input = form.elements[name];
        if (!input || !input.classList) return;
        input.classList.add('is-invalid');
        let fb = input.parentElement.querySelector('.invalid-feedback');
        if (!fb) {
            fb = document.createElement('div');
            fb.className = 'invalid-feedback';
            input.insertAdjacentElement('afterend', fb);
        }
        fb.textContent = message;
    });
}

function renderPagination(container, page, onPage) {
    if (!page || page.totalPages <= 1) { container.innerHTML = ''; return; }
    const cur = page.page, last = page.totalPages - 1;
    const item = (label, target, disabled, active) =>
        `<li class="page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}">` +
        `<a class="page-link" href="#" data-page="${target}">${label}</a></li>`;
    let html = item('&laquo;', cur - 1, cur === 0, false);
    for (let i = Math.max(0, cur - 2); i <= Math.min(last, cur + 2); i++) {
        html += item(i + 1, i, false, i === cur);
    }
    html += item('&raquo;', cur + 1, cur === last, false);
    container.innerHTML = `<nav><ul class="pagination justify-content-center mb-0">${html}</ul></nav>`;
    container.querySelectorAll('[data-page]').forEach(a => a.addEventListener('click', e => {
        e.preventDefault();
        const p = Number(a.dataset.page);
        if (p >= 0 && p <= last && p !== cur) onPage(p);
    }));
}

/* ---------- navbar and page guard ---------- */
function renderNav() {
    const ul = $('navLinks');
    if (!ul) return;
    const u = Auth.user();
    const link = (href, text) => `<li class="nav-item"><a class="nav-link${location.pathname === href ? ' active' : ''}" href="${href}">${text}</a></li>`;
    let html = '';
    if (u && Auth.token()) {
        html += u.role === 'ADMIN'
            ? link('/admin', 'Admin Dashboard')
            : link('/dashboard', 'Dashboard') + link('/jobs', 'Jobs') + link('/applications', 'My Applications') + link('/profile', 'Profile');
        html += `<li class="nav-item"><span class="navbar-text mx-lg-3" id="navName">${esc(u.fullName)}</span></li>` +
            '<li class="nav-item"><button class="btn btn-outline-light btn-sm" id="logoutBtn" type="button">Logout</button></li>';
    } else {
        html += link('/login', 'Login') + '<li class="nav-item"><a class="btn btn-primary btn-sm ms-lg-2" href="/register">Sign up</a></li>';
    }
    ul.innerHTML = html;
    const logout = $('logoutBtn');
    if (logout) logout.addEventListener('click', () => { Auth.clear(); location.href = '/login'; });
}

/** body[data-require] = STUDENT | ADMIN | guest. Returns false when the visitor is being redirected. */
function guard() {
    const need = document.body.dataset.require;
    const u = Auth.user();
    const signedIn = !!(u && Auth.token());
    if (need === 'STUDENT' || need === 'ADMIN') {
        if (!signedIn) { location.replace('/login'); return false; }
        if (u.role !== need) { location.replace(Auth.homeFor(u.role)); return false; }
    }
    if (need === 'guest' && signedIn) { location.replace(Auth.homeFor(u.role)); return false; }
    return true;
}

function onReady(fn) {
    document.addEventListener('DOMContentLoaded', () => {
        renderNav();
        if (guard()) fn();
    });
}
