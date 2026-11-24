/**
 * Testes de ponta a ponta (E2E): sobem a aplicação real numa porta aleatória, com os dados de
 * demonstração, e a usam num Chromium de verdade pelo Playwright Java. Exercitam o que o MockMvc não
 * vê: JavaScript, CSS, celular (RNF-04) e o fluxo completo da demo.
 *
 * <h2>Como rodar</h2>
 * <pre>
 * ./mvnw verify          # NÃO roda E2E (o surefire ignora *E2E.java)
 * ./mvnw verify -Pe2e    # roda também os E2E, pelo failsafe
 * </pre>
 *
 * <h2>Navegador</h2>
 * <p>A classe {@link br.unicamp.var.e2e.E2EBase} escolhe o executável nesta ordem:</p>
 * <ol>
 *   <li>propriedade de sistema {@code e2e.chromium} ou variável de ambiente {@code E2E_CHROMIUM}, com o
 *       caminho de um Chromium/Chrome já instalado, por exemplo
 *       {@code E2E_CHROMIUM=/usr/bin/google-chrome ./mvnw verify -Pe2e};</li>
 *   <li>um Chromium em {@code /opt/pw-browsers/chromium} (arquivo executável ou pasta com o
 *       {@code chrome} dentro), se existir;</li>
 *   <li>senão, a instalação padrão do Playwright, feita uma vez com
 *       {@code ./mvnw -Pe2e test-compile exec:java -Dexec.classpathScope=test
 *       -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"}
 *       (é o que o job {@code e2e} do CI faz).</li>
 * </ol>
 *
 * <h2>Evidências</h2>
 * <p>Cada cenário salva screenshots dos passos, a 360 px e a 1280 px, em
 * {@code target/e2e-screenshots/<Cenario>/}.</p>
 */
package br.unicamp.var.e2e;
