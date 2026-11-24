/*
 * Área do organizador: só melhora as telas, que funcionam sem JavaScript.
 * - Mostra quórum/fração só para maioria qualificada e o 2º turno só para maioria absoluta.
 * - Pede confirmação nas ações do ciclo (form[data-confirmar]).
 * - Imprime os códigos de acesso (button[data-imprimir]).
 */
(function () {
  "use strict";

  var radiosRegra = document.querySelectorAll("input[data-regra]");
  var dependentes = document.querySelectorAll("[data-mostrar-com]");

  function atualizarDependentes() {
    var escolhida = document.querySelector("input[data-regra]:checked");
    var regra = escolhida ? escolhida.value : "";
    dependentes.forEach(function (bloco) {
      bloco.hidden = bloco.getAttribute("data-mostrar-com") !== regra;
    });
  }

  if (radiosRegra.length > 0) {
    radiosRegra.forEach(function (radio) {
      radio.addEventListener("change", atualizarDependentes);
    });
    atualizarDependentes();
  }

  document.querySelectorAll("form[data-confirmar]").forEach(function (formulario) {
    formulario.addEventListener("submit", function (evento) {
      if (!window.confirm(formulario.getAttribute("data-confirmar"))) {
        evento.preventDefault();
      }
    });
  });

  document.querySelectorAll("[data-imprimir]").forEach(function (botao) {
    botao.addEventListener("click", function () {
      window.print();
    });
  });
})();
