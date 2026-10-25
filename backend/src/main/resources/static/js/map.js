/*
 * Mapa principal de Benchify.
 *
 * Estrategia:
 *  - Centrar inicialmente en España (Madrid) con zoom amplio.
 *  - Cuando el usuario mueve el mapa, consultar /api/bancos con el bbox visible.
 *  - Debouncing para evitar saturar el backend en cada drag.
 *  - Cluster mental simple: si hay más de N marcadores, solo renderizar los del bbox.
 *  - Filtros toggle: aplicar en cliente sobre la última respuesta cacheada.
 */
(function () {
    'use strict';

    // ── Estado ────────────────────────────────────────────────────────────
    const state = {
        map: null,
        markersLayer: null,
        bancosCache: [],
        filters: new Set(),
        debounceTimer: null,
        userMarker: null
    };

    // ── Inicialización ────────────────────────────────────────────────────
    function init() {
        const mapEl = document.getElementById('map');
        if (!mapEl) return;

        // Centro inicial: España (zoom amplio)
        state.map = L.map(mapEl, {
            zoomControl: true,
            attributionControl: true
        }).setView([40.4168, -3.7038], 6);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
            maxZoom: 19
        }).addTo(state.map);

        state.markersLayer = L.layerGroup().addTo(state.map);

        // Eventos: cargar bancos cuando se mueve el mapa
        state.map.on('moveend', () => debounce(loadBancos, 300));
        state.map.on('zoomend', () => debounce(loadBancos, 300));

        // Filtros y geolocalización
        bindFilters();
        bindLocateButton();

        // Carga inicial
        loadBancos();
    }

    // ── Debouncing ────────────────────────────────────────────────────────
    function debounce(fn, ms) {
        clearTimeout(state.debounceTimer);
        state.debounceTimer = setTimeout(fn, ms);
    }

    // ── Carga de bancos por bounding box ──────────────────────────────────
    async function loadBancos() {
        const bounds = state.map.getBounds();
        const params = new URLSearchParams({
            minLng: bounds.getWest(),
            minLat: bounds.getSouth(),
            maxLng: bounds.getEast(),
            maxLat: bounds.getNorth()
        });

        const countEl = document.getElementById('map-count');
        if (countEl) countEl.textContent = 'Cargando…';

        try {
            const res = await fetch(`/api/bancos?${params}`);
            if (!res.ok) throw new Error('HTTP ' + res.status);
            state.bancosCache = await res.json();
            renderMarkers();
        } catch (e) {
            console.error('Error cargando bancos:', e);
            if (countEl) countEl.textContent = 'Error al cargar';
        }
    }

    // ── Renderizado de marcadores ─────────────────────────────────────────
    function renderMarkers() {
        state.markersLayer.clearLayers();

        const filtered = applyFilters(state.bancosCache);

        filtered.forEach(banco => {
            const icon = L.divIcon({
                className: 'bench-marker',
                html: '🪑',
                iconSize: [32, 32],
                iconAnchor: [16, 16]
            });

            const marker = L.marker([banco.lat, banco.lng], { icon });
            marker.bindPopup(buildPopupHtml(banco), {
                maxWidth: 280,
                closeButton: true,
                autoPan: true
            });
            marker.addTo(state.markersLayer);
        });

        const countEl = document.getElementById('map-count');
        if (countEl) {
            const total = filtered.length;
            countEl.textContent = total === 0
                ? 'No hay bancos en esta zona'
                : `${total} banco${total === 1 ? '' : 's'} visibles`;
        }
    }

    // ── Popup HTML ────────────────────────────────────────────────────────
    function buildPopupHtml(banco) {
        const stars = window.Benchify?.renderStars(banco.puntuacionMedia) || '';
        const img = banco.primeraImagenUrl
            ? `<img class="popup-banco-img" src="${banco.primeraImagenUrl}" alt="" loading="lazy">`
            : `<div class="popup-banco-img-placeholder">
                 <svg width="40" height="40" viewBox="0 0 24 24" fill="currentColor">
                   <path d="M4 11h16v2H4zm-1 4v4h2v-2h14v2h2v-4zM7 4v6h2V6h6v4h2V4z"/>
                 </svg>
               </div>`;

        const etiquetas = (banco.etiquetas || []).slice(0, 3)
            .map(e => `<span class="tag" style="font-size:10px;padding:2px 8px;">${escapeHtml(e)}</span>`)
            .join('');

        return `
            <div class="popup-banco">
                ${img}
                <div class="popup-banco-body">
                    <h3 class="popup-banco-title">${escapeHtml(banco.titulo)}</h3>
                    <div class="popup-banco-meta">
                        <span class="stars">${stars}</span>
                        <span>${banco.numResenas} reseña${banco.numResenas === 1 ? '' : 's'}</span>
                    </div>
                    <div class="tag-list" style="margin-top:8px;">${etiquetas}</div>
                </div>
                <a class="popup-banco-link" href="/bancos/${banco.id}">Ver ficha completa →</a>
            </div>
        `;
    }

    function escapeHtml(s) {
        return String(s || '').replace(/[&<>"']/g, ch => ({
            '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
        }[ch]));
    }

    // ── Filtros ───────────────────────────────────────────────────────────
    function bindFilters() {
        document.querySelectorAll('.filter-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const key = chip.dataset.filter;
                if (state.filters.has(key)) {
                    state.filters.delete(key);
                    chip.classList.remove('active');
                } else {
                    state.filters.add(key);
                    chip.classList.add('active');
                }
                renderMarkers();
            });
        });
    }

    function applyFilters(bancos) {
        return bancos.filter(b => {
            for (const f of state.filters) {
                if (f === 'con-sombra' && !b.conSombra) return false;
                if (f === 'accesible' && !b.accesible) return false;
                if (f === 'con-respaldo' && !b.conRespaldo) return false;
                if (f === 'puntuacion-4' && (b.puntuacionMedia || 0) < 4) return false;
            }
            return true;
        });
    }

    // ── Geolocalización ───────────────────────────────────────────────────
    function bindLocateButton() {
        const btn = document.getElementById('locate-btn');
        if (!btn) return;

        btn.addEventListener('click', () => {
            if (!navigator.geolocation) {
                window.Benchify?.toast('Tu navegador no soporta geolocalización', 'warning');
                return;
            }
            btn.disabled = true;
            navigator.geolocation.getCurrentPosition(
                pos => {
                    const { latitude, longitude } = pos.coords;
                    state.map.setView([latitude, longitude], 16);

                    if (state.userMarker) state.userMarker.remove();
                    state.userMarker = L.circleMarker([latitude, longitude], {
                        radius: 8,
                        color: '#1F4E3D',
                        fillColor: '#1F4E3D',
                        fillOpacity: 0.4,
                        weight: 3
                    }).addTo(state.map);
                    btn.disabled = false;
                },
                () => {
                    window.Benchify?.toast('No se pudo obtener tu ubicación', 'danger');
                    btn.disabled = false;
                },
                { enableHighAccuracy: true, timeout: 10000 }
            );
        });
    }

    // ── Arranque ──────────────────────────────────────────────────────────
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
