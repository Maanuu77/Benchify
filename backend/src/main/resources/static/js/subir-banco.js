/*
 * Wizard de subida de bancos en 3 pasos.
 *
 *  Paso 1: ubicación (mapa interactivo, clic para fijar coordenadas)
 *  Paso 2: datos (título, descripción, características, etiquetas)
 *  Paso 3: foto (placeholder hasta Fase 4 — R2)
 *
 * Al finalizar envía un POST a /api/bancos con JSON.
 */
(function () {
    'use strict';

    const state = {
        currentStep: 1,
        map: null,
        marker: null,
        etiquetaIds: new Set()
    };

    // ── Navegación entre pasos ────────────────────────────────────────────
    function setStep(n) {
        document.querySelectorAll('[data-wizard-step]').forEach(el => {
            el.classList.toggle('active', parseInt(el.dataset.wizardStep) === n);
        });
        document.querySelectorAll('.step').forEach(el => {
            const stepN = parseInt(el.dataset.step);
            el.classList.toggle('active', stepN === n);
            el.classList.toggle('completed', stepN < n);
        });
        state.currentStep = n;
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function bindNav() {
        document.querySelectorAll('[data-next]').forEach(btn =>
            btn.addEventListener('click', () => {
                if (!validateStep(state.currentStep)) return;
                setStep(state.currentStep + 1);
            })
        );
        document.querySelectorAll('[data-prev]').forEach(btn =>
            btn.addEventListener('click', () => setStep(state.currentStep - 1))
        );
    }

    // ── Validación por paso ───────────────────────────────────────────────
    function validateStep(step) {
        if (step === 1) {
            const lat = document.getElementById('lat').value;
            const lng = document.getElementById('lng').value;
            if (!lat || !lng) {
                window.Benchify?.toast('Selecciona la ubicación del banco en el mapa', 'warning');
                return false;
            }
        }
        if (step === 2) {
            const titulo = document.getElementById('titulo').value.trim();
            if (!titulo) {
                window.Benchify?.toast('El título es obligatorio', 'warning');
                return false;
            }
        }
        return true;
    }

    // ── Mapa de selección de ubicación ────────────────────────────────────
    function initLocationPicker() {
        const el = document.getElementById('location-picker');
        if (!el) return;

        state.map = L.map(el).setView([40.4168, -3.7038], 6);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap', maxZoom: 19
        }).addTo(state.map);

        // Intentar centrar en la ubicación del usuario
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(pos => {
                state.map.setView([pos.coords.latitude, pos.coords.longitude], 16);
            }, () => {}, { enableHighAccuracy: true, timeout: 5000 });
        }

        state.map.on('click', e => {
            const { lat, lng } = e.latlng;
            placeMarker(lat, lng);
            document.getElementById('lat').value = lat;
            document.getElementById('lng').value = lng;
            document.getElementById('coords-display').textContent =
                `📍 Ubicación: ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
        });
    }

    function placeMarker(lat, lng) {
        if (state.marker) {
            state.marker.setLatLng([lat, lng]);
        } else {
            state.marker = L.marker([lat, lng], { draggable: true }).addTo(state.map);
            state.marker.on('dragend', e => {
                const p = e.target.getLatLng();
                document.getElementById('lat').value = p.lat;
                document.getElementById('lng').value = p.lng;
                document.getElementById('coords-display').textContent =
                    `📍 Ubicación: ${p.lat.toFixed(5)}, ${p.lng.toFixed(5)}`;
            });
        }
    }

    // ── Etiquetas seleccionables ──────────────────────────────────────────
    function bindEtiquetas() {
        document.querySelectorAll('#etiquetas-list .tag-selectable').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = parseInt(btn.dataset.etiquetaId);
                if (state.etiquetaIds.has(id)) {
                    state.etiquetaIds.delete(id);
                    btn.classList.remove('selected');
                } else {
                    state.etiquetaIds.add(id);
                    btn.classList.add('selected');
                }
            });
        });
    }

    // ── Envío final ───────────────────────────────────────────────────────
    function bindSubmit() {
        const form = document.getElementById('form-banco');
        if (!form) return;

        form.addEventListener('submit', async e => {
            e.preventDefault();
            if (!validateStep(2)) return;

            const payload = {
                titulo: document.getElementById('titulo').value.trim(),
                descripcion: document.getElementById('descripcion').value.trim() || null,
                lat: parseFloat(document.getElementById('lat').value),
                lng: parseFloat(document.getElementById('lng').value),
                ciudad: document.getElementById('ciudad').value.trim() || null,
                conSombra: form.querySelector('[name="conSombra"]').checked,
                accesible: form.querySelector('[name="accesible"]').checked,
                conRespaldo: form.querySelector('[name="conRespaldo"]').checked,
                material: document.getElementById('material').value || null,
                etiquetaIds: Array.from(state.etiquetaIds)
            };

            const btn = e.submitter || form.querySelector('button[type=submit]');
            btn.disabled = true;
            btn.textContent = 'Publicando…';

            try {
                const res = await fetch('/api/bancos', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });

                if (res.status === 401) {
                    window.location.href = '/auth/login';
                    return;
                }
                if (!res.ok) throw new Error((await res.json()).message || 'Error al crear el banco');

                const banco = await res.json();
                window.Benchify?.toast('¡Banco publicado! Pasará por moderación.', 'success');
                setTimeout(() => window.location.href = '/bancos/' + banco.id, 800);

            } catch (err) {
                window.Benchify?.toast(err.message, 'danger');
                btn.disabled = false;
                btn.textContent = 'Publicar banco';
            }
        });
    }

    // ── Init ──────────────────────────────────────────────────────────────
    function init() {
        bindNav();
        bindEtiquetas();
        bindSubmit();
        initLocationPicker();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
