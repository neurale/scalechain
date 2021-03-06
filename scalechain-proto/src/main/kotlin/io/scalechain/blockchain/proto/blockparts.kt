package io.scalechain.blockchain.proto

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.scalechain.util.*
import io.scalechain.util.HexUtil.bytes
import java.util.*

/** A hash data class that can represent transaction hash or block hash.
  * Used by an inventory vector, InvVector.
  *
  * @param value
  */
data class Hash(val value : Bytes) : Transcodable, Comparable<Hash> {
    init {
        assert(value.array.size > 0)
    }

    fun isAllZero() : Boolean {

        var i = 0
        val valueLength = value.array.size
        while (i < valueLength && value.array.get(i) == 0.toByte()) {
            i += 1
        }
        return i == valueLength
    }

//  fun toHex() : String = value.toString

    override fun toString() = """Hash("${HexUtil.hex(value.array)}")"""

    override operator fun compareTo(other : Hash): Int {
        val value1 = Utils.bytesToBigInteger(this.value.array)
        val value2 = Utils.bytesToBigInteger(other.value.array)

        return value1.compareTo(value2)
    }

    companion object {

        //                   0123456789012345678901234567890123456789012345678901234567890123
        val ALL_ZERO = from("0000000000000000000000000000000000000000000000000000000000000000")

        // BUGBUG : Add test case.
        fun from(hexString : String ) : Hash = Hash(Bytes(bytes(hexString)))
    }
}

// TODO : Json formatting
/*
object HashFormat {
  implicit object hashFormat : RootJsonFormat<Hash> {
    // Instead of { value : "cafebebe" }, we need to serialize the hash to "cafebebe"
    fun write(hash : Hash) = JsString( ByteArray.byteArrayToString(hash.value) )

    // Not used.
    fun read(value:JsValue) {
      assert(false)
      null
    }
  }
}
*/

data class BlockHeader(val version : Int, val hashPrevBlock : Hash, val hashMerkleRoot : Hash, val timestamp : Long, val target : Long, val nonce : Long) : Transcodable {
    override fun toString() : String =
        "BlockHeader(version=$version, hashPrevBlock=$hashPrevBlock, hashMerkleRoot=$hashMerkleRoot, timestamp=${timestamp}L, target=${target}L, nonce=${nonce}L)"

    companion object {
        /** Get the encoded difficulty bits to put into the block header from the minimum block hash.
         *
         * For encoding/decoding the difficulty bits in the block header, see the following link.
         *
         * https://en.bitcoin.it/wiki/Difficulty
         *
         * @param minBlockHash The minimum block hash.
         * @return The encoded difficulty. ( 4 byte integer )
         */
        fun encodeDifficulty(minBlockHash: Hash): Long {
            // TODO : Implement
            assert(false)
            return -1L
        }

        /** Get the minimum block hash from the encoded difficulty bits.
         *
         * For encoding/decoding the difficulty bits in the block header, see the following link.
         *
         * @param target
         * @return
         */
        fun decodeDifficulty(target: Long): Hash {
            // TODO : Implement
            assert(false)
            throw UnsupportedOperationException()
        }
    }
}

data class CoinbaseData(val data: Bytes) : Transcodable {
    override fun toString() : String = "CoinbaseData($data)"
}

interface TransactionInput : Transcodable {
    val outputTransactionHash : Hash
    val outputIndex : Long

    fun getOutPoint() = OutPoint(
        outputTransactionHash,
        outputIndex.toInt()
    )

    /** See if the transaction input data represents the generation transaction input.
     *
     * Generation transaction's UTXO hash has all bits set to zero,
     * and its UTXO index has all bits set to one.
     *
     * @return true if the give transaction input is the generation transaction. false otherwise.
     */
    fun isCoinBaseInput() : Boolean {
        //println(s"${txInput.outputIndex}")
        return outputTransactionHash.isAllZero() && ( outputIndex == 0xFFFFFFFFL )
    }
}

data class NormalTransactionInput(override val outputTransactionHash : Hash,
                                  override val outputIndex : Long,
                                  val unlockingScript : UnlockingScript,
                                  val sequenceNumber : Long) : TransactionInput {
    override fun toString() : String =
        "NormalTransactionInput(outputTransactionHash=$outputTransactionHash, outputIndex=${outputIndex}L, unlockingScript=$unlockingScript, sequenceNumber=${sequenceNumber}L)"
}

data class GenerationTransactionInput(override val outputTransactionHash : Hash,
                                      // BUGBUG : Change to Int
                                      override val outputIndex : Long,
                                      val coinbaseData : CoinbaseData,
                                      val sequenceNumber : Long) : TransactionInput {

    override fun toString(): String =
        "GenerationTransactionInput(outputTransactionHash=$outputTransactionHash, outputIndex=${outputIndex}L, coinbaseData=$coinbaseData, sequenceNumber= ${sequenceNumber}L)"
}

interface Script : Transcodable
{
    val data : Bytes

    // BUGBUG : Changed Interface, name : from length to size
    fun size() = data.array.size
    operator fun get(i:Int) = data.array.get(i)
}

interface LockingScriptPrinter {
    fun toString(lockingScript:LockingScript) : String

    companion object {
        var printer : LockingScriptPrinter? = null
    }
}

data class LockingScript(override val data : Bytes) : Script {

    override fun toString() : String {
        if (LockingScriptPrinter.printer != null)
            return LockingScriptPrinter.printer!!.toString(this)
        else
            return "LockingScript(${HexUtil.kotlinHex(data.array)})"
    }
}



interface UnlockingScriptPrinter {
    fun toString(unlockingScript:UnlockingScript) : String

    companion object {
        var printer : UnlockingScriptPrinter? = null
    }
}

data class UnlockingScript(override val data : Bytes) : Script {
    override fun toString(): String {
        if (UnlockingScriptPrinter.printer != null)
            return UnlockingScriptPrinter.printer!!.toString(this)
        else
            return "UnlockingScript(${HexUtil.kotlinHex(data.array)})"
    }
}

data class TransactionOutput(val value : Long, val lockingScript : LockingScript) : Transcodable {
    override fun toString() : String =
        "TransactionOutput(value=${value}L, lockingScript=$lockingScript)"
}

interface TransactionPrinter {
    fun toString(transaction:Transaction) : String

    companion object {
        var printer : TransactionPrinter? = null
    }
}

/** Tx ; tx describes a bitcoin transaction, in reply to getdata.
  */
data class Transaction(val version : Int,
                       val inputs : List<TransactionInput>,
                       val outputs : List<TransactionOutput>,
                       val lockTime : Long) : ProtocolMessage {

  override fun toString() : String {
    if (TransactionPrinter.printer != null)
        return TransactionPrinter.printer!!.toString(this)
    else
        return "Transaction(version=$version, inputs=listOf(${inputs.joinToString(",")}), outputs=listOf(${outputs.joinToString(",")}), lockTime=${lockTime}L)"
  }
}


/** The block message is sent in response to a getdata message
  * which requests transaction information from a block hash.
  */
data class Block(val header:BlockHeader,
                 val transactions : List<Transaction>) : ProtocolMessage {

    override fun toString() : String =
        "Block(header=$header, transactions=listOf(${transactions.joinToString(",")}))"

    companion object {
        // Need to move these to configurations.
        val MAX_SIZE = 1024 * 1024
    }
}

/** IPv6 address. Network byte order.
  * The original client only supported IPv4 and only read the last 4 bytes to get the IPv4 address.
  * However, the IPv4 address is written into the message as a 16 byte IPv4-mapped IPv6 address
  */
data class IPv6Address(val address : Bytes) : Transcodable {
  override fun toString() : String = "IPv6Address($address)"

  fun inetAddress() = com.google.common.net.InetAddresses.fromLittleEndianByteArray(address.array.reversed().toByteArray())
}

// TODO : Add a comment
// BUGBUG : Changed interface, scala.math.BigInt -> java.math.BigInteger
data class NetworkAddress(val services:java.math.BigInteger, val ipv6:IPv6Address, val port:Int) : Transcodable {
    override fun toString() : String = "NetworkAddress(${BigIntUtil.bint(services)}, $ipv6, $port)"
}
// TODO : Add a comment
data class NetworkAddressWithTimestamp(val timestamp:Long, val address:NetworkAddress) : Transcodable {
    override fun toString() : String = "NetworkAddressWithTimestamp(${timestamp}L, $address)"
}

