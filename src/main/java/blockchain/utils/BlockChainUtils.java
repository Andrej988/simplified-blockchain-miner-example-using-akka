package blockchain.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import blockchain.model.HashResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import blockchain.model.Block;

@NoArgsConstructor(access = AccessLevel.PRIVATE) //To provide a non-instantiable class
public class BlockChainUtils {

	public static String calculateHash(String data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] rawHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < rawHash.length; i++) {
				String hex = Integer.toHexString(0xff & rawHash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
	}

	public static Optional<HashResult> mineBlock(Block block, int difficultyLevel, long startNonce, long endNonce) {
		String hash = new String(new char[difficultyLevel]).replace("\0", "X");
		String target = new String(new char[difficultyLevel]).replace("\0", "0");

		long nonce = startNonce;
		while (!hash.substring(0,difficultyLevel).equals(target) && nonce <= endNonce) {
			nonce++;
			String dataToEncode = block.getPreviousHash() + block.getTransaction().getTimestamp() +
					nonce + block.getTransaction();
			hash = BlockChainUtils.calculateHash(dataToEncode);
		}

		HashResult hashResult = null;
		if (hash.substring(0,difficultyLevel).equals(target)) {
			hashResult = new HashResult();
			hashResult.foundAHash(hash, nonce);
		}
		return Optional.ofNullable(hashResult);
	}
	
	public static boolean validateBlock(Block block) {
		String dataToEncode = block.getPreviousHash() + block.getTransaction().getTimestamp() + block.getNonce() + block.getTransaction();
		String checkHash = calculateHash(dataToEncode);
		return (checkHash.equals(block.getHash()));
	}
}
