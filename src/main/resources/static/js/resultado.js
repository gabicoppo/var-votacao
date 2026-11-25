/*
 * Resultado, relatório, ranking e telões (área do João). As páginas funcionam sem JavaScript; aqui só:
 * - atualização ao vivo da participação ([data-andamento-url]): a cada 10 s nas páginas comuns e a
 *   cada 5 s no telão; recarrega a página comum quando o status muda;
 * - "Momento VAR" ([data-var-revisao]): o overlay some sozinho pelo CSS; clique ou Esc pulam. Na
 *   página comum aparece uma vez por votação em cada aba; no telão ([data-sempre]), sempre;
 * - telão ([data-telao]): tecla F para tela cheia, contagem regressiva, número que "pula" a cada voto
 *   novo e, quando o status muda, troca o conteúdo sem recarregar (a tela cheia continua). No telão da
 *   premiação, o pódio é revelado do 3º ao 1º lugar com espaço, Enter, seta ou clique;
 * - botão de imprimir ([data-imprimir]).
 */
(function () {
  "use strict";

  var INTERVALO_PAGINA_MS = 10000;
  var INTERVALO_TELAO_MS = 5000;
  var TECLAS_PROXIMO = [" ", "Enter", "ArrowRight", "PageDown"];

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

  function buscarAndamento(url) {
    return window.fetch(url, { headers: { Accept: "application/json" }, credentials: "same-origin" })
      .then(function (resposta) { return resposta.ok ? resposta.json() : null; });
  }

  /** Atualiza números e barra; devolve true se o número de votantes aumentou. */
  function atualizarNumeros(raiz, dados) {
    var votantes = raiz.querySelector("[data-votantes]");
    var aumentou = votantes && Number(votantes.textContent) < dados.votantes;
    var texto = percentual(dados.votantes, dados.aptos);
    definirTexto(raiz, "[data-votantes]", String(dados.votantes));
    definirTexto(raiz, "[data-aptos]", String(dados.aptos));
    definirTexto(raiz, "[data-participacao]", texto);
    var barra = raiz.querySelector("[data-barra]");
    if (barra) {
      var largura = texto.replace("%", "").replace(",", ".");
      barra.style.width = largura + "%";
      barra.parentNode.setAttribute("aria-valuenow", largura);
    }
    return aumentou;
  }

  /* ---------- Momento VAR ---------- */
  function prepararRevisao(raiz) {
    var revisao = raiz.querySelector("[data-var-revisao]");
    if (!revisao) {
      return;
    }
    var revelacao = raiz.querySelector("[data-revelacao]");
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
    if (!revisao.hasAttribute("data-sempre")) {
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
    }
    revisao.addEventListener("click", pular);
    document.addEventListener("keydown", function (evento) {
      if (evento.key === "Escape") {
        pular();
      }
    });
  }

  /* ---------- Página comum: andamento a cada 10 s ---------- */
  var andamento = document.querySelector("[data-andamento-url]:not([data-telao])");
  if (andamento && window.fetch) {
    var statusInicial = andamento.getAttribute("data-status");
    window.setInterval(function () {
      buscarAndamento(andamento.getAttribute("data-andamento-url")).then(function (dados) {
        if (!dados) {
          return;
        }
        if (dados.status !== statusInicial) {
          window.location.reload();
          return;
        }
        atualizarNumeros(andamento, dados);
      }).catch(function () { /* tenta de novo no próximo ciclo */ });
    }, INTERVALO_PAGINA_MS);
  }

  var telao = document.querySelector("[data-telao]");
  if (!telao) {
    prepararRevisao(document);
  } else {
    iniciarTelao(telao);
  }

  /* ---------- Telão ---------- */
  function iniciarTelao(inicial) {
    var atual = inicial;
    var contagem = null;
    var podio = null;
    var dica = document.querySelector("[data-dica-tela-cheia]");
    if (dica && document.documentElement.requestFullscreen) {
      dica.hidden = false;
    }

    function preparar(raiz) {
      prepararRevisao(raiz);
      iniciarContagem(raiz);
      podio = prepararPodio(raiz);
      var aoVivo = document.querySelector("[data-ao-vivo]");
      if (aoVivo) {
        aoVivo.hidden = raiz.getAttribute("data-status") === "apurada";
      }
    }

    function iniciarContagem(raiz) {
      if (contagem) {
        window.clearInterval(contagem);
        contagem = null;
      }
      var elemento = raiz.querySelector("[data-contagem]");
      if (!elemento) {
        return;
      }
      var fim = Number(elemento.getAttribute("data-fim"));
      var dois = function (n) { return (n < 10 ? "0" : "") + n; };
      var atualizar = function () {
        var resta = Math.max(0, Math.floor((fim - Date.now()) / 1000));
        if (resta === 0) {
          elemento.textContent = "instantes";
          return;
        }
        var dias = Math.floor(resta / 86400);
        var relogio = dois(Math.floor(resta / 3600) % 24) + ":" + dois(Math.floor(resta / 60) % 60) + ":" +
          dois(resta % 60);
        elemento.textContent = (dias > 0 ? dias + "d " : "") + relogio;
      };
      atualizar();
      contagem = window.setInterval(atualizar, 1000);
    }

    function trocarConteudo() {
      return window.fetch(window.location.href, { credentials: "same-origin" })
        .then(function (resposta) { return resposta.ok ? resposta.text() : null; })
        .then(function (html) {
          if (!html) {
            return;
          }
          var novo = new DOMParser().parseFromString(html, "text/html").querySelector("[data-telao]");
          if (novo) {
            atual.replaceWith(novo);
            atual = novo;
            preparar(atual);
          }
        });
    }

    function consultar() {
      if (atual.getAttribute("data-status") === "apurada") {
        return;
      }
      buscarAndamento(atual.getAttribute("data-andamento-url")).then(function (dados) {
        if (!dados) {
          return;
        }
        if (dados.status !== atual.getAttribute("data-status")) {
          trocarConteudo();
          return;
        }
        if (atualizarNumeros(atual, dados)) {
          var numero = atual.querySelector("[data-votantes]");
          numero.classList.remove("pulou");
          void numero.offsetWidth;
          numero.classList.add("pulou");
        }
      }).catch(function () { /* tenta de novo no próximo ciclo */ });
    }

    document.addEventListener("keydown", function (evento) {
      if (evento.key === "f" || evento.key === "F") {
        if (document.fullscreenElement) {
          document.exitFullscreen();
        } else if (document.documentElement.requestFullscreen) {
          document.documentElement.requestFullscreen().catch(function () { /* recusado pelo navegador */ });
        }
      } else if (podio && TECLAS_PROXIMO.indexOf(evento.key) >= 0 && podio.pendente()) {
        evento.preventDefault();
        podio.proximo();
      }
    });

    preparar(atual);
    if (window.fetch) {
      window.setInterval(consultar, INTERVALO_TELAO_MS);
    }
  }

  /** Pódio com suspense: esconde os degraus e revela um a um, do 3º ao 1º lugar. */
  function prepararPodio(raiz) {
    var area = raiz.querySelector("[data-podio-telao]");
    var degraus = area ? Array.prototype.slice.call(area.querySelectorAll(".degrau")) : [];
    if (degraus.length === 0) {
      return null;
    }
    degraus.sort(function (a, b) { return a.getAttribute("data-passo") - b.getAttribute("data-passo"); });
    var botao = area.querySelector("[data-proximo]");
    var ranking = area.querySelector("[data-ranking-completo]");
    var feitos = 0;
    var rotulo = function () {
      var numero = degraus[feitos].querySelector(".degrau-numero").textContent.trim();
      botao.textContent = "Revelar o " + numero + " lugar (espaço)";
    };
    degraus.forEach(function (degrau) { degrau.classList.add("degrau-oculto"); });
    area.classList.add("podio-suspense");
    ranking.hidden = true;
    botao.hidden = false;
    rotulo();
    var controle = {
      pendente: function () { return feitos < degraus.length; },
      proximo: function () {
        if (!controle.pendente()) {
          return;
        }
        var degrau = degraus[feitos];
        degrau.classList.remove("degrau-oculto");
        degrau.classList.add("degrau-revelado");
        feitos += 1;
        if (controle.pendente()) {
          rotulo();
        } else {
          botao.hidden = true;
          ranking.hidden = false;
          area.classList.add("podio-completo");
          ranking.focus();
        }
      }
    };
    botao.addEventListener("click", controle.proximo);
    return controle;
  }

  document.querySelectorAll("[data-imprimir]").forEach(function (botao) {
    botao.addEventListener("click", function () {
      window.print();
    });
  });
})();
