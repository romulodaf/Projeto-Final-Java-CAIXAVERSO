import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AnalisadorAssincronoDataset {
    private List<Transaction> transacoes;

    public AnalisadorAssincronoDataset(List<Transaction> transacoes) {
        this.transacoes = transacoes;
    }

    public void geraArquivosTXT(){
        //------------------------------------------------------------------------------------------------------------------
        String log = "";
        try (ExecutorService executor = Executors.newFixedThreadPool(2)){
            executor.execute(this::transacoesSuspeitas);
            executor.execute(this::geraArquivoDeEstatisticas);
        } catch (Exception e) {
            System.out.println(e);
        }
        //------------------------------------------------------------------------------------------------------------------

    }
    private void geraArquivoDeEstatisticas(){
        String log = "--- MÉDIA DE SALDO POR PROFISSÃO ---\n";
        log += saldoMedioPorProfissao();
        log += "\n--- VOLUME POR CANAL ---";
        log += transacoesPorCanal();
        log += "\n--- Valores por dia da semana ---\n";
        log += transacoesPorDiaDaSemana();
        try {
            Thread.sleep(5000L);
            Files.write(Path.of("relatorio_estatistico.txt"), log.getBytes());
        } catch (Exception e){
            System.out.println("Erro ao criar o arquivo 'relatorio_estatistico.txt'");
        }
    }
    private void transacoesSuspeitas() {

        String saida = "transaction_id,timestamp,loginAttempts,amount\n";
        List<Transaction> suspeitas = transacoes.stream()
                .filter(v -> v.loginAttempts() > 3 || v.timestamp().getHour() >= 18)
                .toList();
        for (Transaction suspeita : suspeitas){
            saida += suspeita.transactionId() + ",";
            saida += suspeita.timestamp() + ",";
            saida += suspeita.loginAttempts() + ",";
            saida += suspeita.amount() + "\n";
        }
        try {
            Thread.sleep(4000L);
            Files.write(Path.of("transacoes_suspeitas.csv"), saida.getBytes());
        } catch (Exception e){
            System.out.println("Erro ao criar o arquivo 'transacoes_suspeitas.csv'");
        }
    }

    private String saldoMedioPorProfissao(){
        //Busca as profissoes no arquivo
        String saida = "";
        Map<String, List<Transaction>> agrupamentoProfissoes = this.transacoes.stream()
                .collect(Collectors.groupingBy(Transaction::occupation));

        for (String occupation: agrupamentoProfissoes.keySet()){
            double media = agrupamentoProfissoes.get(occupation)
                    .stream()
                    .map(Transaction::balance)
                    .collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            saida += "%s: R$ %,.2f\n".formatted(occupation, media);
        }
        return saida;
    }

    private String transacoesPorCanal(){
        String saida = "";
        Map<Channel, List<Transaction>> agrupamentoCanais = this.transacoes.stream()
                .collect(Collectors.groupingBy(Transaction::channel));

        for (Channel canal: agrupamentoCanais.keySet()){
            long quantidade = agrupamentoCanais.get(canal)
                    .stream()
                    .map(Transaction::channel)
                    .count();
            saida += "%s: %d transações\n".formatted(canal, quantidade);
        }
        return saida;
    }

    private String transacoesPorDiaDaSemana(){
        String saida = "";

        Map<Integer, List<Transaction>> agrupamentoDias = this.transacoes.stream().sorted(Comparator.comparing(Transaction::diaDaSemanaNumero))
                .collect(Collectors.groupingBy(Transaction::diaDaSemanaNumero));

        for (int dia: agrupamentoDias.keySet().stream().sorted().toList()){
            BigDecimal total = calcularValorTotalMovimentado(agrupamentoDias.get(dia));
            saida += "%s: R$ %,.2f \n".formatted(DayOfWeek.of(dia).getDisplayName(TextStyle.FULL, Locale.of("pt", "br")), total.doubleValue());
        }
        return saida;
    }
    private BigDecimal calcularValorTotalMovimentado(List<Transaction> transacoes){
        BigDecimal somaDebitos = transacoes.stream()
                .filter(v -> v.type() == TransactionType.Debit)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaCreditos = transacoes.stream()
                .filter(v -> v.type() == TransactionType.Credit)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return somaCreditos.add(somaDebitos.multiply(BigDecimal.valueOf(-1L)));
    }
}
