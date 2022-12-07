package blockchain.model;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@Data
public class Transaction {
	private UUID id;
	private long Timestamp;
	private int accountNumber;
	private double amount;
}
