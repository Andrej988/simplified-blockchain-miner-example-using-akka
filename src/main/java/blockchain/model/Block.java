package blockchain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@ToString
public class Block {
	private final Transaction transaction;
	private final String previousHash;

	private long nonce;
	private String hash;

	public void setHashAndNonce(HashResult result) {
		this.nonce = result.getNonce();
		this.hash = result.getHash();
	}

	public static Block generateNewBlockWithRandomData(String hashOfPreviousBlock) {
		Transaction transaction = new Transaction(UUID.randomUUID(), getRandomTransactionTime(), getRandomCustomerId(), getRandomAmount());
		return new Block(transaction, hashOfPreviousBlock);
	}

	private static int getRandomCustomerId() {
		return ThreadLocalRandom.current().nextInt(1000, 100000);
	}

	private static double getRandomAmount() {
		return ThreadLocalRandom.current().nextDouble(0.01, 1000000);
	}

	private static long getRandomTransactionTime() {
		LocalDateTime dateTimeFrom = LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0, 0);
		LocalDateTime dateTimeTo = LocalDateTime.now();
		ZonedDateTime zonedDateTimeFrom = ZonedDateTime.of(dateTimeFrom, ZoneId.systemDefault());
		ZonedDateTime zonedDateTimeTo = ZonedDateTime.of(dateTimeTo, ZoneId.systemDefault());

		long startMillis = zonedDateTimeFrom.toInstant().getEpochSecond();
		long endMillis = zonedDateTimeTo.toInstant().getEpochSecond();
		return ThreadLocalRandom.current().nextLong(startMillis, endMillis);
	}
}
