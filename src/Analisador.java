import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public List<Transaction> top10(){
        List<Transaction> top10Transacoes = this.transacoes.stream()
                                                           .sorted(Comparator.comparing(Transaction::amount).reversed())
                                                           .limit(10)
                                                           .toList();
        return top10Transacoes;
    }

    public String saldoMedioPorProfissao(){
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

    public BigDecimal calcularValorTotalMovimentado(){
        BigDecimal somaDebitos = this.transacoes.stream()
                                                .filter(v -> v.type() == TransactionType.Debit)
                                                .map(Transaction::amount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaCreditos = this.transacoes.stream()
                                                .filter(v -> v.type() == TransactionType.Credit)
                                                .map(Transaction::amount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return somaCreditos.add(somaDebitos.multiply(BigDecimal.valueOf(-1L)));
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
