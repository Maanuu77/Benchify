/*
 * Benchify · utilidades globales del cliente.
 * Toasts, menú móvil y helpers reutilizables.
 */
window.Benchify = (function () {

    /* ─── Toast notifications ────────────────────────────────── */
    function ensureContainer() {
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            container.setAttribute('role', 'status');
            container.setAttribute('aria-live', 'polite');
            document.body.appendChild(container);
        }
        return container;
    }

    function toast(message, type = 'info', duration = 3500) {
        const container = ensureContainer();
        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.textContent = message;
        container.appendChild(el);
        setTimeout(() => {
            el.style.transition = 'opacity 200ms';
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 200);
        }, duration);
    }

    /* ─── Menú móvil ────────────────────────────────────────── */
    function initMobileMenu() {
        const toggle = document.querySelector('.mobile-toggle');
        const links = document.querySelector('.nav-links');
        if (!toggle || !links) return;

        toggle.addEventListener('click', () => {
            const expanded = links.classList.toggle('open');
            toggle.setAttribute('aria-expanded', expanded);
        });

        // Cierra el menú al hacer click en un enlace
        links.querySelectorAll('a').forEach(a =>
            a.addEventListener('click', () => links.classList.remove('open'))
        );
    }

    /* ─── Estrellas SVG ─────────────────────────────────────── */
    function renderStars(rating) {
        const full = Math.round(rating);
        let html = '';
        for (let i = 1; i <= 5; i++) {
            const cls = i <= full ? '' : 'star-empty';
            html += `<svg class="${cls}" viewBox="0 0 20 20" aria-hidden="true">
                <path d="M9.05 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.957a1 1 0 00.95.69h4.16c.969 0 1.371 1.24.588 1.81l-3.366 2.446a1 1 0 00-.364 1.118l1.287 3.957c.3.921-.755 1.688-1.54 1.118l-3.366-2.446a1 1 0 00-1.175 0l-3.366 2.446c-.784.57-1.838-.197-1.539-1.118l1.287-3.957a1 1 0 00-.364-1.118L2.05 9.384c-.783-.57-.38-1.81.588-1.81h4.16a1 1 0 00.95-.69l1.286-3.957z"/>
            </svg>`;
        }
        return html;
    }

    function hydrateStars() {
        document.querySelectorAll('[data-stars]').forEach(el => {
            const rating = parseFloat(el.dataset.stars) || 0;
            el.innerHTML = renderStars(rating);
        });
    }

    /* ─── Init ──────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', () => {
        initMobileMenu();
        hydrateStars();
    });

    return { toast, renderStars, hydrateStars };
})();
