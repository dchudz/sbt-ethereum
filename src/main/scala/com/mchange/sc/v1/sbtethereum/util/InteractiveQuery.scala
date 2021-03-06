package com.mchange.sc.v1.sbtethereum.util

import java.io.File

import com.mchange.sc.v1.sbtethereum._

import com.mchange.sc.v1.consuela._
import com.mchange.sc.v1.consuela.ethereum.{EthAddress,EthTransaction}
import com.mchange.sc.v1.consuela.ethereum.specification.Types.Unsigned256

import Parsers._

import scala.annotation.tailrec
import scala.language.higherKinds

private [sbtethereum]
object InteractiveQuery {
  private type I[T] = T

  @tailrec
  private def _queryGoodFile[F[_]]( is : sbt.InteractionService, wrap : File => F[File] )( query : String, goodFile : File => Boolean, notGoodFileRetryPrompt : File => String, noEntryDefault : => F[File] ) : F[File] = {
    val filepath = is.readLine( query, mask = false).getOrElse( throwCantReadInteraction ).trim
    if ( filepath.nonEmpty ) {
      val file = new File( filepath ).getAbsoluteFile()
      if (!goodFile(file)) {
        println( notGoodFileRetryPrompt( file ) )
        _queryGoodFile( is, wrap )( query, goodFile, notGoodFileRetryPrompt, noEntryDefault )
      }
      else {
        wrap( file )
      }
    }
    else {
      noEntryDefault
    }
  }

  private [sbtethereum]
  def queryOptionalGoodFile( is : sbt.InteractionService, query : String, goodFile : File => Boolean, notGoodFileRetryPrompt : File => String ) : Option[File] = {
    _queryGoodFile[Option]( is, Some(_) )( query, goodFile, notGoodFileRetryPrompt, None )
  }


  // not currently used
  private [sbtethereum]
  def queryMandatoryGoodFile( is : sbt.InteractionService, query : String, goodFile : File => Boolean, notGoodFileRetryPrompt : File => String ) : File = {
    _queryGoodFile[I]( is, identity )( query, goodFile, notGoodFileRetryPrompt, queryMandatoryGoodFile( is, query, goodFile, notGoodFileRetryPrompt ) )
  }

  private [sbtethereum]
  def queryYN( is : sbt.InteractionService, query : String ) : Boolean = {
    def prompt = is.readLine( query, mask = false ).get
    def doPrompt : Boolean = {
      def redo = {
        println( "Please enter 'y' or 'n'." )
        doPrompt
      }
      prompt.trim().toLowerCase match {
        case ""          => redo
        case "y" | "yes" => true
        case "n" | "no"  => false
        case _           => redo
      }
    }
    doPrompt
  }

  private [sbtethereum]
  def queryIntOrNone( is : sbt.InteractionService, query : String, min : Int, max : Int ) : Option[Int] = {
    require( min >= 0, "Implementation limitation, only positive numbers are supported for now." )
    require( max >= min, s"max ${max} cannot be smaller than min ${min}." )

    // -1 could not be interpreted as Int, None means empty String
    // this is why we don't support negatives, -1 is out-of-band
    def fetchNum : Option[Int] = { 
      val line = is.readLine( query, mask = false ).getOrElse( throwCantReadInteraction ).trim
      if ( line.isEmpty ) {
        None
      }
      else {
        try {
          Some( line.toInt )
        }
        catch {
          case nfe : NumberFormatException => {
            println( s"Bad entry... '${line}'. Try again." )
            Some(-1)
          }
        }
      }
    }

    def checkRange( num : Int ) = {
      if ( num < min || num > max ) {
        println( s"${num} is out of range. Try again." )
        false
      }
      else {
        true
      }
    }

    @tailrec
    def doFetchNum : Option[Int] = {
      fetchNum match {
        case Some(-1)                          => doFetchNum
        case Some( num ) if !checkRange( num ) => doFetchNum
        case ok                                => ok
      }
    }

    doFetchNum
  }

  // not currently used
  def interactiveQueryUnsignedTransaction( is : sbt.InteractionService, log : sbt.Logger ) : EthTransaction.Unsigned = {
    def queryEthAmount( query : String, nonfunctionalTabHelp : String ) : BigInt = {
      val raw = is.readLine( query, mask = false).getOrElse( throwCantReadInteraction ).trim
      try {
        BigInt(raw)
      }
      catch {
        case nfe : NumberFormatException => {
          sbt.complete.Parser.parse( raw, valueInWeiParser( nonfunctionalTabHelp ) ) match {
            case Left( errorMessage )          => throw new SbtEthereumException( s"Failed to parse amount of ETH to send. Error Message: '${errorMessage}'" )
            case Right( amount ) => amount
          }
        }
      }
    }
    val nonce = {
      val raw = is.readLine( "Nonce: ", mask = false).getOrElse( throwCantReadInteraction ).trim
      BigInt(raw)
    }
    val gasPrice = queryEthAmount( "Gas price (as wei, or else number and unit): ", "<gas-price>" )
    val gasLimit = {
      val raw = is.readLine( "Gas Limit: ", mask = false).getOrElse( throwCantReadInteraction ).trim
      BigInt(raw)
    }
    val to = {
      val raw = is.readLine( "To (as hex address): ", mask = false).getOrElse( throwCantReadInteraction ).trim
      if ( raw.nonEmpty ) Some( EthAddress( raw ) ) else None
    }
    if ( to == None ) log.warn( "No 'To:' address specified. This is a contract creation transaction!" )
    val amount = queryEthAmount( "ETH to send (as wei, or else number and unit): ", "<eth-to-send>" )
    val data = {
      val raw = is.readLine( "Data / Init (as hex string): ", mask = false).getOrElse( throwCantReadInteraction ).trim
      raw.decodeHexAsSeq
    }
    to match {
      case Some( recipient ) => EthTransaction.Unsigned.Message( nonce=Unsigned256( nonce ), gasPrice=Unsigned256(gasPrice), gasLimit=Unsigned256(gasLimit), to=recipient, value=Unsigned256(amount), data=data )
      case None              => EthTransaction.Unsigned.ContractCreation( nonce=Unsigned256( nonce ), gasPrice=Unsigned256(gasPrice), gasLimit=Unsigned256(gasLimit), value=Unsigned256(amount), init=data )
    }
  }
}
