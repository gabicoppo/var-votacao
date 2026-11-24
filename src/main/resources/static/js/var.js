/*
 * VAR — JavaScript comum, incluído pelo layout em todas as páginas. Só melhora a experiência: sem ele,
 * tudo continua funcionando (melhoria progressiva). Sem bibliotecas.
 *
 * 1. Diálogo de confirmação: intercepta QUALQUER form[data-confirmar] e pede confirmação num <dialog>
 *    com o texto do atributo. Atributos opcionais no form:
 *      data-confirmar-titulo  título do diálogo (padrão: "Tem certeza?")
 *      data-confirmar-botao   rótulo do botão de confirmar (padrão: "Confirmar")
 *      data-confirmar-perigo  botão de confirmar vermelho (.btn-perigo), para ações destrutivas
 *    O foco começa em "Cancelar", Esc fecha, e o foco volta ao botão que enviou o form.
 * 2. Faixas com rolagem horizontal (.abas, .navegacao, .tabela, [data-rolagem]): marca
 *    data-mais-esquerda / data-mais-direita para o CSS mostrar um fade onde há conteúdo escondido.
 */
(function () {
  'use strict';

  // ---------- 1. Diálogo de confirmação ----------
  var dialogo = null;
  var pendente = null;

  function criarDialogo() {
    var d = document.createElement('dialog');
    d.className = 'dialogo';
    d.setAttribute('aria-labelledby', 'dialogo-titulo');
    d.setAttribute('aria-describedby', 'dialogo-texto');
    d.innerHTML =
      '<form method="dialog" class="dialogo-conteudo">' +
      '<h2 class="dialogo-titulo" id="dialogo-titulo"></h2>' +
      '<p class="dialogo-texto" id="dialogo-texto"></p>' +
      '<div class="dialogo-acoes">' +
      '<button type="submit" value="cancelar" class="btn btn-secundario" data-dialogo-cancelar>Cancelar</button>' +
      '<button type="submit" value="confirmar" class="btn" data-dialogo-confirmar></button>' +
      '</div></form>';
    document.body.appendChild(d);
    d.addEventListener('close', function () {
      var envio = pendente;
      pendente = null;
      if (!envio) {
        return;
      }
      if (d.returnValue === 'confirmar') {
        enviar(envio.form, envio.botao);
      } else if (envio.botao && envio.botao.focus) {
        envio.botao.focus();
      }
    });
    return d;
  }

  /** Envia sem disparar outro submit (que abriria o diálogo de novo), levando o name/value do botão. */
  function enviar(form, botao) {
    if (botao && botao.name) {
      var campo = document.createElement('input');
      campo.type = 'hidden';
      campo.name = botao.name;
      campo.value = botao.value;
      form.appendChild(campo);
    }
    HTMLFormElement.prototype.submit.call(form);
  }

  function confirmar(form, botao) {
    if (typeof HTMLDialogElement === 'undefined') {
      if (window.confirm(form.getAttribute('data-confirmar'))) {
        enviar(form, botao);
      }
      return;
    }
    dialogo = dialogo || criarDialogo();
    var perigo = form.hasAttribute('data-confirmar-perigo');
    dialogo.classList.toggle('dialogo-perigo', perigo);
    dialogo.querySelector('#dialogo-titulo').textContent =
      form.getAttribute('data-confirmar-titulo') || 'Tem certeza?';
    dialogo.querySelector('#dialogo-texto').textContent = form.getAttribute('data-confirmar');
    var ok = dialogo.querySelector('[data-dialogo-confirmar]');
    ok.textContent = form.getAttribute('data-confirmar-botao') || 'Confirmar';
    ok.className = 'btn ' + (perigo ? 'btn-perigo' : 'btn-primario');
    pendente = { form: form, botao: botao };
    dialogo.returnValue = '';
    dialogo.showModal();
    dialogo.querySelector('[data-dialogo-cancelar]').focus();
  }

  // Fase de captura no document: roda antes dos listeners dos próprios forms (inclusive de JS antigos
  // que ainda chamam window.confirm) e os impede de rodar.
  document.addEventListener('submit', function (evento) {
    var form = evento.target;
    if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-confirmar')) {
      return;
    }
    evento.preventDefault();
    evento.stopImmediatePropagation();
    confirmar(form, evento.submitter || form.querySelector('[type="submit"]'));
  }, true);

  // ---------- 2. Fade nas faixas com rolagem horizontal ----------
  function marcar(faixa) {
    var max = faixa.scrollWidth - faixa.clientWidth;
    faixa.toggleAttribute('data-mais-esquerda', max > 1 && faixa.scrollLeft > 1);
    faixa.toggleAttribute('data-mais-direita', max > 1 && faixa.scrollLeft < max - 1);
  }

  function iniciarFaixas() {
    var faixas = document.querySelectorAll('.abas, .navegacao, .tabela, [data-rolagem]');
    Array.prototype.forEach.call(faixas, function (faixa) {
      marcar(faixa);
      faixa.addEventListener('scroll', function () { marcar(faixa); }, { passive: true });
    });
    window.addEventListener('resize', function () {
      Array.prototype.forEach.call(faixas, marcar);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciarFaixas);
  } else {
    iniciarFaixas();
  }
})();
