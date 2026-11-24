/*
 * Área do organizador. Tudo funciona sem JavaScript; este arquivo só melhora a experiência:
 * - campos da regra que dependem da escolha (quórum e fração; 2º turno);
 * - confirmação das ações do ciclo (form[data-confirmar]);
 * - imprimir e baixar CSV dos códigos de acesso;
 * - copiar o link e o convite;
 * - busca e filtro da lista de presença;
 * - gestão ao vivo de votação aberta (participação e presença a cada 10 s);
 * - formulário de nova votação: prévia das datas, resumo ao vivo e validação imediata.
 */
(function () {
  "use strict";

  var INTERVALO_AO_VIVO = 10000;

  function todos(seletor, raiz) {
    return Array.prototype.slice.call((raiz || document).querySelectorAll(seletor));
  }

  function mostrar(elemento) {
    if (elemento) {
      elemento.hidden = false;
    }
  }

  function linhasPreenchidas(texto) {
    return texto.split(/\r?\n/).filter(function (linha) { return linha.trim() !== ""; });
  }

  function semAcento(texto) {
    return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
  }

  /* ---------- Campos que dependem da regra ---------- */
  todos("form").forEach(function (formulario) {
    var radios = todos("input[data-regra]", formulario);
    if (radios.length === 0) {
      return;
    }
    function atualizar() {
      var escolhida = formulario.querySelector("input[data-regra]:checked");
      var regra = escolhida ? escolhida.value : "";
      todos("[data-mostrar-com]", formulario).forEach(function (bloco) {
        bloco.hidden = bloco.getAttribute("data-mostrar-com") !== regra;
      });
    }
    radios.forEach(function (radio) { radio.addEventListener("change", atualizar); });
    atualizar();
  });

  /* ---------- Confirmação das ações ---------- */
  todos("form[data-confirmar]").forEach(function (formulario) {
    formulario.addEventListener("submit", function (evento) {
      if (!window.confirm(formulario.getAttribute("data-confirmar"))) {
        evento.preventDefault();
      }
    });
  });

  /* ---------- Códigos de acesso: imprimir e baixar CSV ---------- */
  todos("[data-imprimir]").forEach(function (botao) {
    mostrar(botao);
    botao.addEventListener("click", function () { window.print(); });
  });

  function campoCsv(valor) {
    return /[";\n]/.test(valor) ? '"' + valor.replace(/"/g, '""') + '"' : valor;
  }

  todos("[data-baixar-csv]").forEach(function (botao) {
    var tabela = document.querySelector("[data-tabela-acessos]");
    if (!tabela || typeof Blob === "undefined") {
      return;
    }
    mostrar(botao);
    botao.addEventListener("click", function () {
      var linhas = ["eleitor;nome;codigo"];
      todos("tbody tr", tabela).forEach(function (linha) {
        linhas.push(todos("td", linha).map(function (celula) {
          return campoCsv(celula.textContent.trim());
        }).join(";"));
      });
      // BOM para o Excel abrir os acentos certos.
      var arquivo = new Blob(["﻿" + linhas.join("\r\n") + "\r\n"], { type: "text/csv;charset=utf-8" });
      var link = document.createElement("a");
      link.href = URL.createObjectURL(arquivo);
      link.download = botao.getAttribute("data-arquivo") || "codigos-acesso.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(function () { URL.revokeObjectURL(link.href); }, 1000);
    });
  });

  /* ---------- Copiar link e convite ---------- */
  function copiar(campo) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(campo.value);
    }
    return new Promise(function (resolver, rejeitar) {
      var oculto = campo.getAttribute("aria-hidden") === "true";
      if (oculto) {
        campo.removeAttribute("aria-hidden");
      }
      campo.select();
      var ok = document.execCommand && document.execCommand("copy");
      if (oculto) {
        campo.setAttribute("aria-hidden", "true");
      }
      window.getSelection().removeAllRanges();
      if (ok) {
        resolver();
      } else {
        rejeitar();
      }
    });
  }

  todos("[data-copiar-acoes]").forEach(function (grupo) {
    var aviso = grupo.parentNode.querySelector("[data-copiado]");
    var temporizador;
    mostrar(grupo);
    todos("[data-copiar]", grupo).forEach(function (botao) {
      botao.addEventListener("click", function () {
        var campo = document.querySelector(botao.getAttribute("data-copiar"));
        var oque = botao.textContent.indexOf("convite") >= 0 ? "Convite copiado!" : "Link copiado!";
        copiar(campo).then(function () {
          aviso.textContent = "✓ " + oque + " Agora é só colar.";
        }, function () {
          aviso.textContent = "Não foi possível copiar. Selecione o link e copie à mão.";
        });
        clearTimeout(temporizador);
        temporizador = setTimeout(function () { aviso.textContent = ""; }, 4000);
      });
    });
  });

  /* ---------- Lista de presença: busca e filtro ---------- */
  var presenca = document.querySelector("[data-presenca]");
  var filtroAtual = "todos";

  function aplicarFiltroPresenca() {
    if (!presenca) {
      return;
    }
    var campoBusca = presenca.querySelector("[data-presenca-busca]");
    var busca = campoBusca ? semAcento(campoBusca.value.trim()) : "";
    var linhas = todos("[data-eleitor]", presenca);
    var contagem = { todos: linhas.length, votou: 0, pendente: 0 };
    var visiveis = 0;
    linhas.forEach(function (linha) {
      var votou = linha.getAttribute("data-votou") === "true";
      contagem[votou ? "votou" : "pendente"] += 1;
      var texto = semAcento(linha.getAttribute("data-id") + " " + linha.getAttribute("data-nome"));
      var passaFiltro = filtroAtual === "todos" || (filtroAtual === "votou") === votou;
      var aparece = passaFiltro && texto.indexOf(busca) >= 0;
      linha.hidden = !aparece;
      if (aparece) {
        visiveis += 1;
      }
    });
    Object.keys(contagem).forEach(function (chave) {
      var contador = presenca.querySelector('[data-contador="' + chave + '"]');
      if (contador) {
        contador.textContent = contagem[chave];
      }
    });
    var resumo = presenca.querySelector("[data-presenca-resumo]");
    if (resumo) {
      resumo.textContent = visiveis === linhas.length ? "" : "Mostrando " + visiveis + " de " + linhas.length + ".";
    }
    var vazia = presenca.querySelector("[data-presenca-vazia]");
    if (vazia) {
      vazia.hidden = visiveis > 0;
    }
  }

  if (presenca) {
    mostrar(presenca.querySelector("[data-presenca-filtros]"));
    var busca = presenca.querySelector("[data-presenca-busca]");
    if (busca) {
      busca.addEventListener("input", aplicarFiltroPresenca);
    }
    todos("[data-filtro]", presenca).forEach(function (botao) {
      botao.addEventListener("click", function () {
        filtroAtual = botao.getAttribute("data-filtro");
        todos("[data-filtro]", presenca).forEach(function (outro) {
          outro.setAttribute("aria-pressed", String(outro === botao));
        });
        aplicarFiltroPresenca();
      });
    });
  }

  /* ---------- Gestão ao vivo (votação aberta) ---------- */
  function percentual(fracao) {
    var valor = Math.round(fracao * 1000) / 10;
    return valor.toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
  }

  function atualizarPresenca(votaram) {
    var rotuloPendente = presenca.getAttribute("data-rotulo-pendente") || "Pendente";
    var conjunto = {};
    votaram.forEach(function (id) { conjunto[id] = true; });
    todos("[data-eleitor]", presenca).forEach(function (linha) {
      var votou = conjunto[linha.getAttribute("data-id")] === true;
      linha.setAttribute("data-votou", String(votou));
      var selo = linha.querySelector("[data-selo]");
      selo.textContent = votou ? "Votou" : rotuloPendente;
      selo.classList.toggle("votou", votou);
      selo.classList.toggle("pendente", !votou);
    });
    aplicarFiltroPresenca();
  }

  var gestao = document.querySelector("[data-gestao-ao-vivo]");
  if (gestao && window.fetch) {
    var statusInicial = gestao.getAttribute("data-status");
    mostrar(gestao.querySelector("[data-indicador-ao-vivo]"));
    mostrar(gestao.querySelector("[data-aviso-ao-vivo]"));
    var buscarJson = function (url) {
      return fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
        .then(function (resposta) {
          if (!resposta.ok) {
            throw new Error("HTTP " + resposta.status);
          }
          return resposta.json();
        });
    };
    var atualizar = function () {
      if (document.hidden) {
        return;
      }
      buscarJson(gestao.getAttribute("data-url-andamento")).then(function (andamento) {
        if (andamento.status !== statusInicial) {
          window.location.reload();
          return null;
        }
        var fracao = andamento.aptos > 0 ? andamento.votantes / andamento.aptos : 0;
        var valor = gestao.querySelector("[data-participacao-valor]");
        var novo = percentual(fracao);
        if (valor.textContent.trim() !== novo) {
          valor.textContent = novo;
        }
        gestao.querySelector("[data-participacao-texto]").textContent =
          andamento.votantes + " de " + andamento.aptos + " aptos votaram";
        gestao.querySelector("[data-participacao-barra]").style.width = (fracao * 100).toFixed(1) + "%";
        return buscarJson(gestao.getAttribute("data-url-presenca"));
      }).then(function (dados) {
        if (dados && presenca) {
          atualizarPresenca(dados.votaram);
        }
      }).catch(function () {
        // Sem rede por um instante: tenta de novo no próximo ciclo.
      });
    };
    setInterval(atualizar, INTERVALO_AO_VIVO);
    document.addEventListener("visibilitychange", function () {
      if (!document.hidden) {
        atualizar();
      }
    });
  }

  /* ---------- Formulário de nova votação ---------- */
  var formulario = document.querySelector("[data-formulario-votacao]");
  if (!formulario) {
    return;
  }

  function dataLegivel(valor) {
    var partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(valor || "");
    return partes ? partes[3] + "/" + partes[2] + "/" + partes[1] + " às " + partes[4] + ":" + partes[5] : null;
  }

  function erroDoCampo(campo, mensagem) {
    var bloco = campo.closest(".campo");
    var existente = bloco.querySelector("[data-erro-js]");
    if (!mensagem) {
      if (existente) {
        existente.remove();
        if (!bloco.querySelector(".erro-campo")) {
          bloco.classList.remove("campo-com-erro");
        }
        campo.removeAttribute("aria-invalid");
      }
      return;
    }
    if (!existente) {
      existente = document.createElement("p");
      existente.className = "erro-campo";
      existente.setAttribute("data-erro-js", "");
      existente.id = campo.id + "-erro-js";
      bloco.appendChild(existente);
      var descricao = campo.getAttribute("aria-describedby") || "";
      if (descricao.indexOf(existente.id) < 0) {
        campo.setAttribute("aria-describedby", (descricao + " " + existente.id).trim());
      }
    }
    // Some com o erro do servidor do mesmo campo, para não mostrar duas mensagens.
    todos(".erro-campo:not([data-erro-js])", bloco).forEach(function (antigo) { antigo.remove(); });
    existente.textContent = mensagem;
    bloco.classList.add("campo-com-erro");
    campo.setAttribute("aria-invalid", "true");
  }

  function mensagemObrigatorio(campo) {
    var minimo = parseInt(campo.getAttribute("data-minimo-linhas") || "1", 10);
    var preenchido = campo.tagName === "TEXTAREA"
      ? linhasPreenchidas(campo.value).length >= minimo
      : campo.value.trim() !== "";
    return preenchido ? null : campo.getAttribute("data-obrigatorio");
  }

  var inicio = formulario.querySelector("#inicio");
  var fim = formulario.querySelector("#fim");

  function erroPeriodo() {
    if (inicio.value && fim.value && fim.value <= inicio.value) {
      return "O fim do período deve ser depois do início.";
    }
    return null;
  }

  function atualizarDatas() {
    todos("[data-previa-de]", formulario).forEach(function (previa) {
      var campo = formulario.querySelector("#" + previa.getAttribute("data-previa-de"));
      var legivel = dataLegivel(campo.value);
      previa.textContent = legivel ? "" : "";
      if (legivel) {
        var forte = document.createElement("strong");
        forte.textContent = legivel;
        previa.append(previa.getAttribute("data-prefixo") + " ", forte, " (horário de Brasília)");
      }
    });
    erroDoCampo(fim, erroPeriodo());
  }

  [inicio, fim].forEach(function (campo) {
    campo.addEventListener("input", atualizarDatas);
    campo.addEventListener("change", atualizarDatas);
  });
  atualizarDatas();

  todos("[data-obrigatorio]", formulario).forEach(function (campo) {
    campo.addEventListener("blur", function () { erroDoCampo(campo, mensagemObrigatorio(campo)); });
    campo.addEventListener("input", function () {
      if (campo.closest(".campo").querySelector("[data-erro-js]")) {
        erroDoCampo(campo, mensagemObrigatorio(campo));
      }
    });
  });

  formulario.addEventListener("submit", function (evento) {
    if (evento.submitter && evento.submitter.name === "usarDemonstracao") {
      return;
    }
    var primeiro = null;
    todos("[data-obrigatorio]", formulario).forEach(function (campo) {
      var mensagem = mensagemObrigatorio(campo);
      erroDoCampo(campo, mensagem);
      if (mensagem && !primeiro) {
        primeiro = campo;
      }
    });
    var periodo = erroPeriodo();
    erroDoCampo(fim, periodo);
    if (periodo && !primeiro) {
      primeiro = fim;
    }
    if (primeiro) {
      evento.preventDefault();
      primeiro.focus();
    }
  });

  /* Resumo ao vivo */
  var resumo = document.querySelector("[data-resumo]");
  if (!resumo) {
    return;
  }
  mostrar(resumo);
  var urlRegra = formulario.getAttribute("data-url-regra");
  var esperaRegra;

  function textoResumo(chave, texto) {
    resumo.querySelector("[data-resumo-" + chave + "]").textContent = texto;
  }

  function atualizarRegra() {
    clearTimeout(esperaRegra);
    esperaRegra = setTimeout(function () {
      var params = new URLSearchParams();
      ["tipoRegra", "quorum", "fracaoExigida"].forEach(function (nome) {
        var campo = formulario.querySelector('[name="' + nome + '"]:checked, input[type=text][name="' + nome + '"]');
        if (campo) {
          params.append(nome, campo.value);
        }
      });
      var segundo = formulario.querySelector('input[type=checkbox][name="segundoTurno"]');
      params.append("segundoTurno", segundo && segundo.checked ? "true" : "false");
      fetch(urlRegra + "?" + params.toString(), { credentials: "same-origin" })
        .then(function (resposta) { return resposta.ok ? resposta.json() : null; })
        .then(function (dados) {
          if (dados) {
            textoResumo("regra", dados.descricao);
            todos("[data-descricao-regra]", formulario).forEach(function (el) { el.textContent = dados.descricao; });
          }
        })
        .catch(function () { /* mantém o texto anterior */ });
    }, 250);
  }

  function atualizarResumo() {
    var pergunta = formulario.querySelector("#pergunta").value.trim();
    textoResumo("pergunta", pergunta || "—");
    var opcoes = linhasPreenchidas(formulario.querySelector("#opcoes").value).length;
    textoResumo("opcoes", opcoes === 1 ? "1 opção (mínimo 2)" : opcoes + " opções");
    var sigilo = formulario.querySelector('input[name="sigilo"]:checked');
    textoResumo("sigilo", sigilo ? sigilo.parentNode.querySelector("strong").textContent.replace(/^\W+/, "") : "—");
    var abre = dataLegivel(inicio.value);
    var fecha = dataLegivel(fim.value);
    textoResumo("periodo", abre && fecha ? abre + " até " + fecha : "Sem período (abre e encerra manualmente)");
    var eleitores = linhasPreenchidas(formulario.querySelector("#eleitores").value).length;
    textoResumo("eleitores", eleitores === 1 ? "1 eleitor" : eleitores + " eleitores");
  }

  formulario.addEventListener("input", function (evento) {
    atualizarResumo();
    if (/^(quorum|fracaoExigida)$/.test(evento.target.name)) {
      atualizarRegra();
    }
  });
  formulario.addEventListener("change", function (evento) {
    atualizarResumo();
    if (/^(tipoRegra|segundoTurno)$/.test(evento.target.name)) {
      atualizarRegra();
    }
  });
  atualizarResumo();
})();
