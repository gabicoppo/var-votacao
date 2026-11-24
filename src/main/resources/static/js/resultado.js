/*
 * Resultado, relatório e ranking (área do João). As páginas funcionam sem JavaScript; aqui só:
 * - atualização ao vivo da participação ([data-andamento-url]), a cada 10 s; recarrega quando o
 *   status muda (ex.: virou "apurada");
 * - "Momento VAR" ([data-var-revisao]): o overlay some sozinho pelo CSS; o clique ou o Esc pulam, e
 *   ele aparece uma vez por votação em cada aba;
 * - botão de imprimir ([data-imprimir]).
 */
(function () {
  "use strict";

  var INTERVALO_MS = 10000;

  function percentual(votantes, aptos) {
    if (aptos <= 0) {
      return "0%";
    }
    var valor = Math.min(100, (votantes * 100) / aptos);
    return valor.toFixed(1).replace(/\.0$/, "").replace(".", ",") + "%";
  }

  function definirTexto(raiz, seletor, texto) {
    var elemento = raiz.querySelector(seletor);
    if (elemento) {
      elemento.textContent = texto;
    }
  }

  var andamento = document.querySelector("[data-andamento-url]");
  if (andamento && window.fetch) {
    var url = andamento.getAttribute("data-andamento-url");
    var statusInicial = andamento.getAttribute("data-status");
    window.setInterval(function () {
      window.fetch(url, { headers: { Accept: "application/json" }, credentials: "same-origin" })
        .then(function (resposta) { return resposta.ok ? resposta.json() : null; })
        .then(function (dados) {
          if (!dados) {
            return;
          }
          if (dados.status !== statusInicial) {
            window.location.reload();
            return;
          }
          var texto = percentual(dados.votantes, dados.aptos);
          definirTexto(andamento, "[data-votantes]", String(dados.votantes));
          definirTexto(andamento, "[data-aptos]", String(dados.aptos));
          definirTexto(andamento, "[data-participacao]", texto);
          var barra = andamento.querySelector("[data-barra]");
          if (barra) {
            barra.style.width = texto.replace("%", "").replace(",", ".") + "%";
            barra.parentNode.setAttribute("aria-valuenow", texto.replace("%", "").replace(",", "."));
          }
        })
        .catch(function () { /* tenta de novo no próximo ciclo */ });
    }, INTERVALO_MS);
  }

  var revisao = document.querySelector("[data-var-revisao]");
  var revelacao = document.querySelector("[data-revelacao]");
  if (revisao) {
    var pulada = false;
    var pular = function () {
      if (pulada) {
        return;
      }
      pulada = true;
      revisao.classList.add("var-revisao-pulada");
      if (revelacao) {
        revelacao.classList.add("revelacao-imediata");
      }
    };
    var chave = "var-revisao-" + revisao.getAttribute("data-votacao");
    try {
      if (window.sessionStorage.getItem(chave)) {
        pular();
      } else {
        window.sessionStorage.setItem(chave, "1");
      }
    } catch (e) {
      /* sem sessionStorage: mostra a revisão normalmente */
    }
    revisao.addEventListener("click", pular);
    document.addEventListener("keydown", function (evento) {
      if (evento.key === "Escape") {
        pular();
      }
    });
  }

  document.querySelectorAll("[data-imprimir]").forEach(function (botao) {
    botao.addEventListener("click", function () {
      window.print();
    });
  });
})();
