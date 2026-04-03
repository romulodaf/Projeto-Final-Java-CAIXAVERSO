import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;


public record Transaction(String transactionId,
                          String accountId,
                          BigDecimal amount,
                          LocalDateTime timestamp,
                          TransactionType type,
                          Channel channel,
                          String occupation,
                          int loginAttempts,
                          BigDecimal balance) {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMATTER_SAIDA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Transaction fromCSV(String row) {
        String[] campos = row.split(",");

        return new Transaction(campos[0],
                campos[1],
                new BigDecimal(campos[2]),
                LocalDateTime.parse(campos[3], FORMATTER),
                TransactionType.valueOf(campos[4]),
                Channel.valueOf(campos[9]),
                campos[11],
                Integer.parseInt(campos[13]),
                new BigDecimal(campos[14]));
    }

    public String diaDaSemana(){
        //return String.valueOf(Arrays.stream(Locale.getAvailableLocales()).map(Locale::toString).filter().toList());
        return this.timestamp.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("pt", "br"));
    }
    public int diaDaSemanaNumero(){
        //return String.valueOf(Arrays.stream(Locale.getAvailableLocales()).map(Locale::toString).filter().toList());
        return this.timestamp.getDayOfWeek().getValue();
    }
}