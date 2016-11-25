package io.scalechain.blockchain.script

import java.math.BigInteger
import io.scalechain.blockchain.proto.Script
import io.scalechain.blockchain.{ScriptEvalException, ScriptParseException, ErrorCode}
import io.scalechain.blockchain.script.ops.ScriptOp
import io.scalechain.util.Utils
import org.scalatest.{Suite, Matchers}

import scala.Long
import scala.collection.mutable.ArrayBuffer

/**
 * Created by kangmo on 11/10/15.
 */
class InvalidStackValueException : Exception
class InvalidExpectationTypeException : Exception

trait OperationTestTrait : Matchers {
  this: Suite =>

  /** Verify a stack with expected values.
   *
   * @param subject The context of the stack. Just for debugging purpose.
   * @param actualStack The actual script stack.
   * @param expectedValues A list of expected values.
   */
  protected fun verifyStack(subject : String, actualStack:ScriptStack, expectedValues : Array<ScriptValue>) : Unit {
    //println ("We are comparing the stack. subject : " + subject)
    //println ("stack : " + actualStack)

    for (i <- 0 until expectedValues.length) {
      val actualOutput = actualStack(expectedValues.length-1-i)
      val expectedOutput = expectedValues(i)

      //println (s"expected output : ${expectedOutput}" )
      //println (s"actual output : ${actualOutput}" )

      actualOutput should be (expectedOutput)
    }
  }

  /** Verify an operation without alt stack inputs and outputs.
   *
   * @param inputs initial values on the main stack.
   * @param operations the list of operation to verify
   * @param expectation final values on the main stack or an error code.
   */
  protected fun verifyOperations( inputs : Array<ScriptValue>,
                                 operations : List<ScriptOp>,
                                 expectation : AnyRef,
                                 serializeAndExecute : Boolean = false
                                 ) : Unit {
    verifyOperationsWithAltStack(inputs, null, operations, expectation, null, serializeAndExecute);
  }

  /** Push an array of values on to a stack.
   *
   * @param stack The stack where values are pushed.
   * @param values The array of ScriptValues(s) to push.
   */
  protected fun pushValues(stack : ScriptStack, values : Array<ScriptValue>): Unit {
    for ( value : ScriptValue <-values) {
      stack.push( value )
    }
  }

  /** Verify an operation with alt stack inputs and outputs.
   *
   * @param mainStackInputs initial values on the main stack.
   * @param altStackInputs initial values on the alt stack.
   * @param operations list of operations to verify
   * @param expectation final values on the main stack or an error code.
   * @param altStackOutputs final values on the alt stack.
   */
  protected fun verifyOperationsWithAltStack(
                  mainStackInputs : Array<ScriptValue>,
                  altStackInputs : Array<ScriptValue>,
                  operations : List<ScriptOp>,
                  expectation : AnyRef,
                  altStackOutputs : Array<ScriptValue>,
                  serializeAndExecute : Boolean = false
                ) : Unit {
    //println (s"Testing with input ${mainStackInputs.mkString(",")}" )

    // Arithmetic operations do not use script chunk, so it is ok to pass null for the parsed script.
    val env = ScriptEnvironment()


    pushValues(env.stack, mainStackInputs)

    if (altStackInputs != null)
      pushValues(env.altStack, altStackInputs)

    fun executeOps() : Unit {

      val operationsToExecute =
        if (serializeAndExecute) {
          // Serialze and parse the serialized byte array to get the pseudo operations such as OpCond,
          // which is generated from OP_IF/OP_NOTIF, OP_ELSE, OP_ENDIF during parsing.
          val serializedOperations = ScriptSerializer.serialize(operations)
          ScriptParser.parse(Script(serializedOperations)).operations
        } else {
          operations
        }

      for (op : ScriptOp <- operationsToExecute) {
        //println (s"Executing operation : ${op}" )
        op.execute(env)
      }
    }

    expectation match {
      case exception : ScriptParseException => {
        val thrown = the<ScriptParseException> thrownBy {
          executeOps()
        }
        thrown.code should be (exception.code)
      }
      // BUGBUG :  Get rid of this pattern, change all test case to use the above pattern.
      case errorCode : ErrorCode => {
        val thrown = the<ScriptEvalException> thrownBy {
          executeOps()
        }
        thrown.code should be (errorCode)
      }
      case expectedOutputValues : Array<ScriptValue> => {
        executeOps()

        //println ("expected values (main) :" + expectedOutputValues.mkString(","))
        if (altStackOutputs != null)
        //println ("expected values (alt) :" + altStackOutputs.mkString(","))


        verifyStack("actual main stack", env.stack, expectedOutputValues)

        if (altStackOutputs != null)
          verifyStack("actual alt stack",  env.altStack, altStackOutputs)
      }
      case _ => throw InvalidExpectationTypeException()
    }
  }

  /** Create an array of ScriptValue(s) from the items argument.
   *
   * @param items The list of arguments which is converted to an array of ScriptValue(s)
   * @return The array of ScriptValues(s)
   */
  fun stack(items : Any* ) : Array<ScriptValue> {
  //  val buffer = ArrayBuffer<ScriptValue>()
    val scriptValues = for (item : Any <- items) yield {
      item match {
        case value : ByteArray => ScriptValue.valueOf(value)
        case value : Int => ScriptValue.valueOf(value)
        case value : Long => ScriptValue.valueOf(value)
        case value : String => ScriptValue.valueOf(value)
        case _ => throw InvalidStackValueException()
      }
    }
    scriptValues.toArray
  }

  /** Create ScriptOpList which has a list of ScriptOp(s)
   *
   * @param operations The list of operations
   * @return The ScriptOpList
   */
  fun ops(operations:ScriptOp*) : ScriptOpList {
    val ops = for(o : ScriptOp <- operations) yield o
    ScriptOpList( ops.toList )
  }
}

