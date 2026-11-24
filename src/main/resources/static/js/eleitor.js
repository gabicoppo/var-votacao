// Área do eleitor (histórias #23, #26 e #28). Melhoria progressiva: sem JS, tudo funciona com os
// formulários normais; este arquivo só melhora a experiência.
(function () {
  'use strict';

  function todos(seletor, raiz) {
    return Array.prototype.slice.call((raiz || document).querySelectorAll(seletor));
  }

  // ---------- Cédula da votação: "Revisar voto" só habilita depois de escolher ----------
  todos('form[data-cedula]').forEach(function (form) {
    var radios = todos('input[type="radio"]', form);
    var botao = form.querySelector('[data-revisar]');
    var dica = form.querySelector('[data-dica]');
    if (!botao || radios.length === 0) {
      return;
    }
    function atualizar() {
      var escolheu = radios.some(function (r) { return r.checked; });
      botao.disabled = !escolheu;
      if (dica) {
        dica.hidden = escolheu;
      }
    }
    radios.forEach(function (r) { r.addEventListener('change', atualizar); });
    atualizar();
  });

  // ---------- Confirmar: desabilita o botão e mostra "Registrando…" (evita duplo clique) ----------
  todos('[data-registrando]').forEach(function (botao) {
    var form = botao.form;
    if (!form) {
      return;
    }
    form.addEventListener('submit', function () {
      // Adia um instante para o envio já ter começado quando o botão for desabilitado.
      window.setTimeout(function () {
        botao.disabled = true;
        botao.setAttribute('aria-busy', 'true');
        botao.textContent = 'Registrando…';
      }, 0);
    });
  });

  // ---------- Cédula Top 3: cartões de ranking sincronizados com os 3 selects ----------
  var LUGARES = ['1º', '2º', '3º'];
  var MEDALHAS = ['🥇', '🥈', '🥉'];

  todos('form[data-top3]').forEach(function (form) {
    var campos = form.querySelector('[data-campos-top3]');
    var ranking = form.querySelector('[data-ranking]');
    var selects = todos('select[data-lugar]', form);
    var cartoes = todos('[data-candidato]', form);
    var vagas = todos('[data-vaga]', form);
    var contador = form.querySelector('[data-contador]');
    var anuncio = form.querySelector('[data-anuncio]');
    var limpar = form.querySelector('[data-limpar]');
    var revisar = form.querySelector('[data-revisar-top3]');
    if (!campos || !ranking || selects.length !== 3 || cartoes.length === 0) {
      return;
    }

    // Estado: candidatos na ordem escolhida (no máximo 3), lido dos selects (ex.: voltando da revisão).
    var escolhidos = [];
    selects.forEach(function (s) {
      if (s.value && escolhidos.indexOf(s.value) < 0) {
        escolhidos.push(s.value);
      }
    });

    function faltam() {
      return 3 - escolhidos.length;
    }

    function textoContador() {
      if (faltam() === 0) {
        return 'Pronto! Confira o pódio e revise a cédula.';
      }
      if (escolhidos.length === 0) {
        return 'Toque nos jogadores, na ordem: 1º, 2º e 3º.';
      }
      return 'Escolha mais ' + faltam() + '.';
    }

    function desenhar() {
      selects.forEach(function (s, i) { s.value = escolhidos[i] || ''; });
      cartoes.forEach(function (cartao) {
        var nome = cartao.getAttribute('data-candidato');
        var posicao = escolhidos.indexOf(nome);
        var selo = cartao.querySelector('[data-selo]');
        cartao.setAttribute('aria-pressed', posicao >= 0 ? 'true' : 'false');
        cartao.setAttribute('aria-label', posicao >= 0 ? nome + ', ' + LUGARES[posicao] + ' lugar' : nome);
        cartao.className = 'candidato-top3' + (posicao >= 0 ? ' lugar-' + (posicao + 1) : '');
        if (selo) {
          selo.textContent = posicao >= 0 ? MEDALHAS[posicao] + ' ' + LUGARES[posicao] : '';
        }
      });
      vagas.forEach(function (vaga, i) {
        var nome = vaga.querySelector('[data-vaga-nome]');
        vaga.classList.toggle('preenchida', Boolean(escolhidos[i]));
        if (nome) {
          nome.textContent = escolhidos[i] || LUGARES[i] + ' lugar';
        }
      });
      if (contador) {
        contador.textContent = textoContador();
        contador.classList.toggle('pronto', faltam() === 0);
      }
      if (limpar) {
        limpar.disabled = escolhidos.length === 0;
      }
      if (revisar) {
        revisar.disabled = faltam() !== 0;
      }
    }

    function anunciar(texto) {
      if (anuncio) {
        anuncio.textContent = texto + ' ' + textoContador();
      }
    }

    cartoes.forEach(function (cartao) {
      cartao.addEventListener('click', function () {
        var nome = cartao.getAttribute('data-candidato');
        var posicao = escolhidos.indexOf(nome);
        if (posicao >= 0) {
          escolhidos.splice(posicao, 1);
          desenhar();
          anunciar(nome + ' saiu do pódio.');
        } else if (faltam() === 0) {
          anunciar('Você já escolheu 3. Toque num escolhido para tirá-lo.');
        } else {
          escolhidos.push(nome);
          desenhar();
          anunciar(nome + ' em ' + LUGARES[escolhidos.length - 1] + ' lugar.');
        }
      });
    });

    if (limpar) {
      limpar.addEventListener('click', function () {
        escolhidos = [];
        desenhar();
        anunciar('Cédula limpa.');
        cartoes[0].focus();
      });
    }

    campos.hidden = true;
    ranking.hidden = false;
    desenhar();
  });

  // ---------- Comprovante: salvar (imprimir/PDF), compartilhar e copiar ----------
  var aviso = document.querySelector('.copiado');
  function avisar(texto) {
    if (aviso) {
      aviso.textContent = texto;
    }
  }

  function copiar(texto) {
    if (!navigator.clipboard) {
      return;
    }
    navigator.clipboard.writeText(texto).then(function () { avisar('Código copiado.'); });
  }

  todos('[data-copiar]').forEach(function (botao) {
    var alvo = document.querySelector(botao.getAttribute('data-copiar'));
    if (!alvo || !navigator.clipboard) {
      return;
    }
    // Com Web Share, "Compartilhar" já cobre o caso; "Copiar código" é o plano B.
    botao.hidden = typeof navigator.share === 'function';
    botao.addEventListener('click', function () { copiar(alvo.textContent.trim()); });
  });

  todos('[data-salvar]').forEach(function (botao) {
    var dica = document.querySelector('[data-dica-salvar]');
    botao.hidden = false;
    if (dica) {
      dica.hidden = false;
    }
    botao.addEventListener('click', function () { window.print(); });
  });

  todos('[data-compartilhar]').forEach(function (botao) {
    var cartao = botao.closest('.comprovante');
    var codigo = document.querySelector('#codigo-comprovante');
    if (!cartao || !codigo || typeof navigator.share !== 'function') {
      return;
    }
    botao.hidden = false;
    botao.addEventListener('click', function () {
      var cod = codigo.textContent.trim();
      navigator.share({
        title: 'Comprovante de voto · VAR',
        text: 'Meu comprovante na votação "' + cartao.getAttribute('data-pergunta') + '": ' + cod
            + '. Ele não revela em quem votei.',
        url: cartao.getAttribute('data-link')
      }).catch(function (erro) {
        if (!erro || erro.name !== 'AbortError') {
          copiar(cod);
        }
      });
    });
  });
})();
