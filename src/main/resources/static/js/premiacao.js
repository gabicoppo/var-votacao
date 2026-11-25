/*
 * Área de premiações do organizador. As telas funcionam sem JavaScript; aqui só o botão de imprimir os
 * códigos de acesso ([data-imprimir]). A confirmação das ações do ciclo (form[data-confirmar]) é o
 * diálogo comum do var.js.
 */
(function () {
  "use strict";

  document.querySelectorAll("[data-imprimir]").forEach(function (botao) {
    botao.addEventListener("click", function () {
      window.print();
    });
  });
})();
