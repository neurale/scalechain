package io.scalechain.blockchain.api.command.network.p2

import io.scalechain.blockchain.api.command.RpcCommand
import io.scalechain.blockchain.api.domain.RpcError
import io.scalechain.blockchain.api.domain.RpcRequest
import io.scalechain.blockchain.api.domain.RpcResult
import io.scalechain.util.Either
import io.scalechain.util.Either.Left
import io.scalechain.util.Either.Right

/*
  CLI command :
    # Try connecting to the following node.
    bitcoin-cli -testnet addnode 192.0.2.113:18333 onetry

  CLI output :
    (no output from bitcoin-cli because result is set to null)

  Json-RPC request :
    {"jsonrpc": "1.0", "id":"curltest", "method": "addnode", "params": [] }

  Json-RPC response :
    {
      "result": null ,
      "error": null,
      "id": "curltest"
    }
*/

/** AddNode: attempts to add or remove a node from the addnode list, or to try a connection to a node once.
  *
  * https://bitcoin.org/en/developer-reference#addnode
  */
object AddNode : RpcCommand() {
  override fun invoke(request : RpcRequest) : Either<RpcError, RpcResult?> {
    // TODO : Implement
    assert(false)
    return Right(null)
  }
  override fun help() : String =
    """addnode "node" "add|remove|onetry"
      |
      |Attempts add or remove a node from the addnode list.
      |Or try a connection to a node once.
      |
      |Arguments:
      |1. "node"     (string, required) The node (see getpeerinfo for nodes)
      |2. "command"  (string, required) 'add' to add a node to the list, 'remove' to remove a node from the list, 'onetry' to try a connection to the node once
      |
      |Examples:
      |> bitcoin-cli addnode "192.168.0.6:8333" "onetry"
      |> curl --user myusername --data-binary '{"jsonrpc": "1.0", "id":"curltest", "method": "addnode", "params": ["192.168.0.6:8333", "onetry"] }' -H 'content-type: text/plain;' http://127.0.0.1:8332/
    """.trimMargin()
}


