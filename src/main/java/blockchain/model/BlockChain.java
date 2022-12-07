package blockchain.model;

import java.util.LinkedList;

import blockchain.utils.BlockChainUtils;

public class BlockChain {
	private final LinkedList<Block> blocks;

	public BlockChain() {
		blocks = new LinkedList<>();
	}
	
	public void addBlock(Block block) throws BlockValidationException {
		String lastHash = "0";
		
		if (blocks.size() > 0) {
			lastHash = blocks.getLast().getHash();
		}
		
		if (!lastHash.equals(block.getPreviousHash())) {
			throw new BlockValidationException();
		}
		
		if (!BlockChainUtils.validateBlock(block)) {
			throw new BlockValidationException();
		}
		
		blocks.add(block);
	}
	
	public void printAndValidate() {
		String lastHash = "0";
		int blockNumber = 0;
		for (Block block : blocks) {
			System.out.println("Block " + blockNumber + ": " + block.getTransaction().getId() + " hash: " + block.getHash());
			System.out.println(block.getTransaction());
			
			if (block.getPreviousHash().equals(lastHash)) {
				System.out.print("Last hash matches ");
			} else {
				System.out.print("Last hash doesn't match ");
			}
			
			if (BlockChainUtils.validateBlock(block)) {
				System.out.println("and hash is valid");
			} else {
				System.out.println("and hash is invalid");
			}
			
			lastHash = block.getHash();
			blockNumber++;
			
		}
	}

	public String getLastHash() {
		return blocks.size() > 0 ? blocks.getLast().getHash() : null;
	}

	public int getSize() {
		return blocks.size();
	}
}
