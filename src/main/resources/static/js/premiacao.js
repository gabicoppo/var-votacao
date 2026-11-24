/*
 * Área de premiações do organizador. As telas funcionam sem JavaScript; aqui só:
 * - confirmação nas ações do ciclo (form[data-confirmar]);
 * - botão de imprimir os códigos de acesso ([data-imprimir]).
 */
(function () {
  "use strict";

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
