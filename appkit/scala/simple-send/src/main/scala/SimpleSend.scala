import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.ErgoToolConfig
import org.ergoplatform.appkit.impl.ErgoTreeContract

object SimpleSend {

  def main(args: Array[String]): Unit = {
    val toolConfig = ErgoToolConfig.load("config.json")
    val nodeConfig = toolConfig.getNode()
    val ergoClient = RestApiErgoClient.create(nodeConfig, RestApiErgoClient.defaultTestnetExplorerUrl)
    val transactionInfo = ergoClient.execute((ctx: BlockchainContext) => {
      val prover = ctx.newProverBuilder
        .withMnemonic(
          SecretString.create(nodeConfig.getWallet.getMnemonic),
          SecretString.create(nodeConfig.getWallet.getPassword)
        )
        .withEip3Secret(0)
        .build()
      
      val senderAddress = prover.getEip3Addresses().get(0)
      val boxesToSpend = ctx.getUnspentBoxesFor(senderAddress, 0, 20)
      
      val outBox = ctx.newTxBuilder.outBoxBuilder
        .value(Parameters.OneErg)
        .contract(new ErgoTreeContract(senderAddress.getErgoAddress().script, ctx.getNetworkType))
        .build()

      val tx = ctx.newTxBuilder
        .boxesToSpend(boxesToSpend)
        .outputs(outBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(senderAddress.getErgoAddress())
        .build()

      val signed = prover.sign(tx)
      val transactionInfo = signed.toJson(true)
      ctx.sendTransaction(signed)
      transactionInfo
    })
    println(transactionInfo)
  }
  
}