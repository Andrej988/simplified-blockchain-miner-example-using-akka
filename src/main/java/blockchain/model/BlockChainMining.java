package blockchain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class BlockChainMining {
    @Getter
    private long startTime;

    @Getter
    private int numberOfBlocksToMine;

    private int workloadPerWorker;

    @Getter
    private int difficulty;

    private BlockChain blockchain;

    @Getter
    @Setter
    private Block currentBlock;

    @Getter
    @Setter
    private long nextStartNonce;

    @Getter
    @Setter
    private boolean assignNewBlock;

    public int getBlockChainSize() {
        return this.blockchain.getSize();
    }

    public String getHashOfPreviousBlock() {
        return blockchain.getSize() > 0 ? blockchain.getLastHash() : "0";
    }

    public long calculateEndNonce() {
        return this.nextStartNonce + this.workloadPerWorker - 1;
    }

    public void increaseStartNonce() {
        this.nextStartNonce += workloadPerWorker;
    }

    public void addCurrentBlockToBlockChain(HashResult hashResult)  {
        this.currentBlock.setHashAndNonce(hashResult);
        try {
            this.blockchain.addBlock(currentBlock);
        } catch (BlockValidationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException("Blockchain Validation Error!!!");
        }
    }

    public void printAndValidateBlockChain() {
        this.blockchain.printAndValidate();
    }

    public boolean isActualBlock(Block block) {
        return !this.assignNewBlock && currentBlock.equals(block);
    }

}
