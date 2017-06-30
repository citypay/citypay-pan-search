package com.citypay.pan.search.util

import java.io.BufferedReader

/**
  * Used to provide credentials to a protected resource
  */
trait CredentialsCallback {

  def getUsername: String
  def getPassword: String

}

class CommandLineCredentials extends CredentialsCallback {

  override def getUsername: String = {
    System.console().readLine("Database Username: ")
  }

  override def getPassword: String = {
    new String(System.console().readPassword("Database Password: "))
  }

}

class NoOpCredentials extends CredentialsCallback {
  override def getUsername: String = null
  override def getPassword: String = null
}