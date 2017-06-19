import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {



    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        ArrayList<UTXO> claimedUtxos = new ArrayList<>();

        float sumOfInputs = 0; // sum of outputs claimed by inputs
        float sumOfOutputs = 0;

        // (1) all outputs claimed by {@code tx} are in the current UTXO pool,
        // for o in all tx.i if utxo.contains(o)
        // Outputs claimed by transactions are outputs in the UTXO referenced by transactions inputs

        // for all transaction inputs
        for (Transaction.Input txIn : tx.getInputs()) {

            // Construct a claimedUtxo to test "contains" in the UTXOPool
            UTXO claimedUtxo = new UTXO(txIn.prevTxHash, txIn.outputIndex);

            // If not every utxo referenced by an input in the transaction is in the UTXOPool, "isValidTx" fails
            if (!utxoPool.contains(claimedUtxo))
                return false;

            // (2) the signatures on each input of {@code tx} are valid,
            // for all i in tx.i if Crypto.verifySignature(o.pk, tx.getRawDataToSig, i.sig)

            // Get the transaction that generated the claimed utxo
            Transaction.Output claimedOutput = utxoPool.getTxOutput(claimedUtxo);

            // Verify signature
            if (!Crypto.verifySignature(claimedOutput.address, tx.getRawDataToSign(tx.getInputs().indexOf(txIn)), txIn.signature))
                return false;

            // (3) no UTXO is claimed multiple times by {@code tx}
            if(claimedUtxos.contains(claimedUtxo))
              return false;

            // add claimedOutput value to the amount of input values to this transaction
            sumOfInputs += claimedOutput.value;

            // add claimedUtxo to the claimed utxo list for the next inputs to
            // verify double spending within this transaction
            claimedUtxos.add(claimedUtxo);
        }


        // (4) all of {@code tx}s output values are non-negative
        for (Transaction.Output txOut : tx.getOutputs()) {

            if (txOut.value < 0) {
                return false;
            }
            sumOfOutputs += txOut.value;
        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        // values; and false otherwise.
        if(!(sumOfInputs >= sumOfOutputs))
          return false;

        return true;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction [] possibleTxs) {
        // IMPLEMENT THIS

        ArrayList<Transaction> validTx = validateGroup(new ArrayList<>(Arrays.asList(possibleTxs)));

        // update utxoPool

        for(Transaction tx : validTx) {

        }

        return (Transaction[]) validTx.toArray();

    }

    private ArrayList<Transaction> validateGroup(ArrayList<Transaction> group) {

        ArrayList<Transaction> acceptedTx = new ArrayList<>();
  //      UTXOPool tempUtxoPool = new UTXOPool();

        // validate each transaction individually with the current UtxoPool
        for (Transaction tx : group) {
            if (isValidTx(tx)) {
                acceptedTx.add(tx);
                group.remove(tx);
            }
        }

        for (Transaction tx : acceptedTx) {
            // test if accepted transactions claims the same utxo
            for (Transaction.Input txIn : tx.getInputs()) {
                UTXO claimedUtxo = new UTXO(txIn.prevTxHash, txIn.outputIndex);

                if (utxoPool.contains(claimedUtxo)) {
                    // output was not claimed yet

                    // add utxo to temporary utxoPool to verify double spending within other accepted tx
                    //utxoPool.addUTXO(claimedUtxo, utxoPool.getTxOutput(claimedUtxo));

                    // remove spent utxo from utxoPool
                    // in this way we accept the first transaction which claims that utxo
                    utxoPool.removeUTXO(claimedUtxo);

                } else {
                    // in this case it is a double spending
                    // for now lets just drop the transaction from accepted
                    acceptedTx.remove(tx);
                    break;
                }
            }
        }

        if (acceptedTx.size() > 0) {

            // update utxoPartially adding new utxo created by accepted transactions
            for (Transaction tx : acceptedTx) {
                for (Transaction.Output txOut : tx.getOutputs()) {
                    UTXO newUtxo = new UTXO(tx.getHash(), tx.getOutputs().indexOf(txOut));
                    utxoPool.addUTXO(newUtxo, txOut);
                }
            }

            acceptedTx.addAll(validateGroup(group));
            return acceptedTx;
        }
        else
            return acceptedTx;

    }

}
