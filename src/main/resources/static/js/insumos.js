var UNIDADES = {
    'solido':  [['kg','Kilogramos (kg)'], ['g','Gramos (g)']],
    'liquido': [['l','Litros (l)'], ['ml','Mililitros (ml)']],
    'otro':    [['unidad','Unidades']]
};

function actualizarUnidades(tipo, seleccionar) {
    var select = document.getElementById('selectUnidad');
    select.innerHTML = '';
    UNIDADES[tipo].forEach(function(par) {
        var opt = document.createElement('option');
        opt.value = par[0];
        opt.textContent = par[1];
        if (seleccionar && par[0] === seleccionar) {
            opt.selected = true;
        }
        select.appendChild(opt);
    });
}