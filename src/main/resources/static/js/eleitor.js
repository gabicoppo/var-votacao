// Área do eleitor: copiar o código do comprovante e não repetir candidatos na cédula Top 3.
(function () {
  'use strict';

  document.querySelectorAll('[data-copiar]').forEach(function (botao) {
    botao.addEventListener('click', function () {
      var alvo = document.querySelector(botao.getAttribute('data-copiar'));
      var aviso = document.querySelector('.copiado');
      if (!alvo || !navigator.clipboard) {
        return;
      }
      navigator.clipboard.writeText(alvo.textContent.trim()).then(function () {
        if (aviso) {
          aviso.textContent = 'Código copiado.';
        }
      });
    });
  });

  var selects = Array.prototype.slice.call(document.querySelectorAll('select[data-top3]'));
  function atualizar() {
    var escolhidos = selects.map(function (s) { return s.value; });
    selects.forEach(function (select, indice) {
      Array.prototype.forEach.call(select.options, function (opcao) {
        var usado = opcao.value !== '' && escolhidos.some(function (v, i) { return i !== indice && v === opcao.value; });
        opcao.hidden = usado;
        opcao.disabled = usado;
      });
    });
  }
  selects.forEach(function (s) { s.addEventListener('change', atualizar); });
  atualizar();
})();
