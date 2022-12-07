package blockchain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class HashResult {
	@EqualsAndHashCode.Include
	@ToString.Include
	private long nonce;

	@EqualsAndHashCode.Include
	@ToString.Include
	private String hash;

	private boolean complete = false;

	public synchronized void foundAHash(String hash, long nonce) {
		this.hash = hash;
		this.nonce = nonce;
		this.complete = true;
	}
	
}
