/*
 * HTMX configuration: enviar el token CSRF automáticamente en todas las
 * peticiones no seguras (POST/PUT/DELETE/PATCH).
 *
 * Spring Security expone XSRF-TOKEN como cookie (CookieCsrfTokenRepository
 * con withHttpOnlyFalse), y espera el header X-XSRF-TOKEN en la respuesta.
 */
(function () {
    function getCookie(name) {
        const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
        return match ? decodeURIComponent(match[2]) : null;
    }

    document.body.addEventListener('htmx:configRequest', function (event) {
        const token = getCookie('XSRF-TOKEN');
        if (token) {
            event.detail.headers['X-XSRF-TOKEN'] = token;
        }
    });

    document.body.addEventListener('htmx:responseError', function (event) {
        const status = event.detail.xhr.status;
        if (status === 401) {
            window.location.href = '/auth/login';
        } else if (status >= 500) {
            window.Benchify?.toast?.('Error del servidor. Inténtalo de nuevo.', 'danger');
        }
    });
})();
