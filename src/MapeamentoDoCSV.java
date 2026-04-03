import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapeamentoDoCSV {

    public static void main(String[] args) {
        //código para importar o arquivo para a variável "transascoes" do tipo Transaction
        //------------------------------------------------------------------------------------------------------------------
        List<Transaction> transacoes = new ArrayList<>();
        try (BufferedReader leitor = Files.newBufferedReader(Path.of("bank_transactions_data_2.csv"))) {
            transacoes = leitor.lines()
                    .filter(valor -> valor.contains("TransactionID") == false)//Não processa a linha com os nomes dos campos
                    .map(Transaction::fromCSV)
                    .toList();

        } catch (Exception e) {
            System.out.println(e);
        }
        //------------------------------------------------------------------------------------------------------------------


        //Cria um objeto analisador passando as transações para o construtor
        //------------------------------------------------------------------------------------------------------------------
        Analisador analisador = new Analisador(transacoes);
        AnalisadorAssincronoDataset criadorDeArquivos = new AnalisadorAssincronoDataset(transacoes);
        //------------------------------------------------------------------------------------------------------------------
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.execute(criadorDeArquivos::geraArquivosTXT);
            System.out.println("==================================================\n   RELATÓRIO DE TRANSAÇÕES BANCÁRIAS - SUMÁRIO\n==================================================");
            System.out.println("Quantidade total de transaões processadas: " + analisador.getQuantidadeDeTransacoes());
            System.out.printf("Valor total movimentado: R$ %.2f\n", analisador.calcularValorTotalMovimentadoComFuture());
            System.out.println("\n--------------------------------------------------\n" + "TOP 10 MAIORES TRANSAÇÕES:\n");
            System.out.println(analisador.top10());
        } catch (Exception e){
            System.out.println("Ocorreu um erro.");
        }


    }
}

