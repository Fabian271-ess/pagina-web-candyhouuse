var btn   = document.getElementById('notifBtn');
var panel = document.getElementById('notifPanel');
var badge = document.getElementById('notifBadge');
var lista = document.getElementById('notifLista');

btn.addEventListener('click', function(e) {
    e.stopPropagation();
    panel.classList.toggle('open');
});

document.addEventListener('click', function() {
    panel.classList.remove('open');
});

panel.addEventListener('click', function(e) {
    e.stopPropagation();
});

function cargarNotificaciones() {
    fetch('/api/notificaciones')
        .then(function(res) { return res.json(); })
        .then(function(data) {

            var total   = data.total   || 0;
            var alertas = data.alertas || [];

            if (total > 0) {
                badge.textContent = total;
                badge.classList.remove('hidden');
            } else {
                badge.classList.add('hidden');
            }

            if (alertas.length === 0) {
                lista.innerHTML =
                    '<p style="padding:20px;text-align:center;color:#aaa;font-size:13px;">Todo en orden.</p>';
                return;
            }

            var html = '';

            for (var i = 0; i < alertas.length; i++) {

                var a = alertas[i];

                var color =
                    a.tipo === 'producto' ? '#e35d7a' :
                        a.tipo === 'insumo'   ? '#e5a93f' :
                            '#4fbf88';

                html += '<a href="' + a.link + '" ' +
                    'style="display:block;padding:12px 16px;border-bottom:1px solid #fce8ed;text-decoration:none;background:white;">';

                html += '<div style="font-size:11px;font-weight:bold;color:' + color + ';text-transform:uppercase;margin-bottom:4px;">'
                    + a.tipo + '</div>';

                html += '<div style="font-size:13px;color:#5a3a33;">'
                    + a.mensaje + '</div>';

                html += '</a>';
            }

            lista.innerHTML = html;
        })
        .catch(function() {
            lista.innerHTML =
                '<p style="padding:20px;text-align:center;color:#aaa;">Error al cargar.</p>';
        });
}

cargarNotificaciones();
setInterval(cargarNotificaciones, 30000);