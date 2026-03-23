/**
 * datetime.js — Candy House
 * Widget de hora y fecha en tiempo real.
 * Se inyecta como tercer elemento del .topbar para quedar centrado
 * con justify-content: space-between (logo | widget | botones).
 */
(function () {

    var DIAS  = ['domingo','lunes','martes','miércoles','jueves','viernes','sábado'];
    var MESES = ['enero','febrero','marzo','abril','mayo','junio',
        'julio','agosto','septiembre','octubre','noviembre','diciembre'];
    var CLOCKS = ['🕛','🕐','🕑','🕒','🕓','🕔','🕕','🕖','🕗','🕘','🕙','🕚'];

    function injectWidget() {
        var topbar = document.querySelector('.topbar');
        if (!topbar) return;

        var w = document.createElement('div');
        w.className = 'dt-widget';
        w.innerHTML =
            '<div class="dt-icon-wrap" id="dt-icon">🕐</div>' +
            '<div class="dt-body">' +
            '<div class="dt-time">' +
            '<span id="dt-hm">--:--</span>' +
            '<span class="dt-sec" id="dt-sec">:--</span>' +
            '<span class="dt-ampm" id="dt-ampm"></span>' +
            '</div>' +
            '</div>' +
            '<div class="dt-sep"></div>' +
            '<div class="dt-date-block">' +
            '<span class="dt-dayname" id="dt-day">---</span>' +
            '<span class="dt-fulldate" id="dt-date">-- de --- de ----</span>' +
            '</div>';

        /* Insertar como segundo hijo directo del topbar
           → logo | widget | botones  →  space-between lo centra solo */
        var children = topbar.children;
        if (children.length >= 2) {
            topbar.insertBefore(w, children[1]);
        } else {
            topbar.appendChild(w);
        }
    }

    function tick() {
        var now  = new Date();
        var h    = now.getHours();
        var m    = String(now.getMinutes()).padStart(2, '0');
        var s    = String(now.getSeconds()).padStart(2, '0');
        var ampm = h >= 12 ? 'PM' : 'AM';
        h = h % 12 || 12;

        var hm   = document.getElementById('dt-hm');
        var sec  = document.getElementById('dt-sec');
        var ap   = document.getElementById('dt-ampm');
        var day  = document.getElementById('dt-day');
        var date = document.getElementById('dt-date');
        var icon = document.getElementById('dt-icon');

        if (hm)   hm.textContent   = String(h).padStart(2,'0') + ':' + m;
        if (sec)  sec.textContent  = ':' + s;
        if (ap)   ap.textContent   = ampm;
        if (day)  day.textContent  = DIAS[now.getDay()];
        if (date) date.textContent =
            now.getDate() + ' de ' + MESES[now.getMonth()] + ' de ' + now.getFullYear();
        if (icon) icon.textContent = CLOCKS[now.getHours() % 12];
    }

    function init() { injectWidget(); tick(); setInterval(tick, 1000); }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();