import javax.swing.text.DateFormatter;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Analisador {
    private List<Transaction> transacoes;
    private int quantidadeDeThreads = 10;
    private List<List<Transaction>> partes;
    private long quantidadeDeTransacoes;

    public Analisador(List<Transaction> transacoes) {
        this.transacoes = transacoes;
        this.quantidadeDeTransacoes = transacoes.size();
    }

    public Analisador(List<Transaction> transacoes, int quantidadeDeThreads) {
        this.transacoes = transacoes;
        this.quantidadeDeThreads = quantidadeDeThreads;
        this.quantidadeDeTransacoes = transacoes.size();
    }

    private void dividirPartes(){
        partes = new ArrayList<>();
        int indiceInicial = 0;
        int tamanho = transacoes.size();
        Double incremento = Math.ceil((double) tamanho / quantidadeDeThreads);
        for (int i = 0; i < quantidadeDeThreads; i++) {
            int indiceFinal = indiceInicial + incremento.intValue();
            if (indiceFinal > tamanho) {
                indiceFinal = tamanho;
            }
            List<Transaction> parte = transacoes.subList(indiceInicial, indiceFinal);
            partes.add(parte);
            indiceInicial = indiceFinal;
        }
    }

    public String top10(){
        List<Transaction> top10Transacoes = this.transacoes.stream()
                                                           .sorted(Comparator.comparing(Transaction::amount).reversed())
                                                           .limit(10)
                                                           .toList();
        String saida = "";
        int cont = 1;
        for(Transaction transacao : top10Transacoes){
            saida += cont + ": ";
            saida += "ID: " + transacao.transactionId() + " | ";
            saida += "Valor: R$ %,.2f".formatted(transacao.amount()) + " | ";
            saida += "Conta: " + transacao.accountId() + " | ";
            saida += "Data: " + transacao.timestamp().format(Transaction.FORMATTER_SAIDA);
            saida += "\n";
            cont++;
        }
        return saida;
    }





    public BigDecimal calcularValorTotalMovimentadoComFuture(){
        dividirPartes();
        BigDecimal totalMovimentado;
        try (ExecutorService executor = Executors.newFixedThreadPool(quantidadeDeThreads)){
            List<Future<BigDecimal>> somaDebitosLista = new ArrayList<>();
            List<Future<BigDecimal>> somaCreditosLista = new ArrayList<>();
            BigDecimal somaDebitosValor = BigDecimal.ZERO;
            BigDecimal somaCreditosValor = BigDecimal.ZERO;
            for(List<Transaction> parte : partes){
                Future<BigDecimal> somaDebitos = executor.submit(() -> parte.stream()
                                                                            .filter(v -> v.type() == TransactionType.Debit)
                                                                            .map(Transaction::amount)
                                                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                Future<BigDecimal> somaCreditos = executor.submit(() -> parte.stream()
                                                                            .filter(v -> v.type() == TransactionType.Credit)
                                                                            .map(Transaction::amount)
                                                                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                somaDebitosLista.add(somaDebitos);
                somaCreditosLista.add(somaCreditos);
            }
            for (int i = 0; i < quantidadeDeThreads; i++){
                somaDebitosValor = somaDebitosValor.add(safeGet(somaDebitosLista.get(i)));
                somaCreditosValor = somaCreditosValor.add(safeGet(somaCreditosLista.get(i)));
            }
            //System.out.println("Total de Débitos: " + somaDebitosValor.multiply(BigDecimal.valueOf(-1L)));
            //System.out.println("Total de Créditos: " + somaCreditosValor);
            totalMovimentado = somaCreditosValor.add(somaDebitosValor.multiply(BigDecimal.valueOf(-1L)));
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        return totalMovimentado;
    }

    public long getQuantidadeDeTransacoes() {
        return quantidadeDeTransacoes;
    }

    private static BigDecimal safeGet(Future<BigDecimal> bigDecimalFuture) {
        try {
            return bigDecimalFuture.get();
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao buscar a soma", ex);
        }
    }
}
