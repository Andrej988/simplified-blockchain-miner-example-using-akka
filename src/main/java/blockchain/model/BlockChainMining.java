package blockchain.model;

import akka.actor.typed.ActorRef;
import blockchain.actors.Worker;
import blockchain.actors.WorkerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Map<ActorRef<Worker.Command>, WorkerStatus> workers;

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

    public void initWorkers(Set<ActorRef<Worker.Command>> workersSet) {
        if(workers == null) {
            workers = new HashMap<>();
        }
        workersSet.forEach(worker -> this.workers.put(worker, WorkerStatus.IDLE));
    }

    public void setWorkerStatus(ActorRef<Worker.Command> worker, WorkerStatus status) {
        workers.put(worker, status);
    }

    public WorkerStatus getWorkerStatus(ActorRef<Worker.Command> worker) {
        return workers.get(worker);
    }

    public Set<ActorRef<Worker.Command>> getAllWorkers() {
        return new HashSet<>(workers.keySet());
    }

    public Set<ActorRef<Worker.Command>> getIdleWorkers() {
        return workers.entrySet()
                .stream()
                .filter(x -> x.getValue() == WorkerStatus.IDLE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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
